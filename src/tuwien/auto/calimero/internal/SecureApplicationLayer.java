/*
    Calimero - A library for KNX network access
    Copyright (c) 2019, 2020 B. Malinowsky

    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program; if not, write to the Free Software
    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA

    Linking this library statically or dynamically with other modules is
    making a combined work based on this library. Thus, the terms and
    conditions of the GNU General Public License cover the whole
    combination.

    As a special exception, the copyright holders of this library give you
    permission to link this library with independent modules to produce an
    executable, regardless of the license terms of these independent
    modules, and to copy and distribute the resulting executable under terms
    of your choice, provided that you also meet, for each linked independent
    module, the terms and conditions of the license of that module. An
    independent module is a module which is not derived from or based on
    this library. If you modify this library, you may extend this exception
    to your version of the library, but you are not obligated to do so. If
    you do not wish to do so, delete this exception statement from your
    version.
*/

package tuwien.auto.calimero.internal;

import static java.lang.String.format;

import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntUnaryOperator;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.slf4j.Logger;

import tuwien.auto.calimero.CloseEvent;
import tuwien.auto.calimero.DataUnitBuilder;
import tuwien.auto.calimero.FrameEvent;
import tuwien.auto.calimero.GroupAddress;
import tuwien.auto.calimero.IndividualAddress;
import tuwien.auto.calimero.KNXAddress;
import tuwien.auto.calimero.KNXException;
import tuwien.auto.calimero.KNXFormatException;
import tuwien.auto.calimero.KNXIllegalArgumentException;
import tuwien.auto.calimero.KNXTimeoutException;
import tuwien.auto.calimero.KnxSecureException;
import tuwien.auto.calimero.Priority;
import tuwien.auto.calimero.ReturnCode;
import tuwien.auto.calimero.SecurityControl;
import tuwien.auto.calimero.SecurityControl.DataSecurity;
import tuwien.auto.calimero.cemi.CEMIFactory;
import tuwien.auto.calimero.cemi.CEMILData;
import tuwien.auto.calimero.link.KNXLinkClosedException;
import tuwien.auto.calimero.link.KNXNetworkLink;
import tuwien.auto.calimero.link.NetworkLinkListener;
import tuwien.auto.calimero.log.LogService;

/**
 * Secure application layer for KNX data security.
 */
public class SecureApplicationLayer implements AutoCloseable {

	public static final int SecureService = 0b1111110001;

	protected static final int InvalidScf = 1;
	protected static final int SeqNoError = 2;
	protected static final int CryptoError = 3;
	protected static final int AccessAndRoleError = 4;

	static boolean test;

	static final int SecureDataPdu = 0;
	static final int SecureSyncRequest = 2;
	static final int SecureSyncResponse = 3;

	private static final int MacSize = 4;
	private static final int SeqSize = 6;

	private static final String secureSymbol = new String(Character.toChars(0x1F512));


	private final KNXNetworkLink link;
	private final byte[] serialNumber;
	private final Logger logger;

	// local sequences
	private volatile long sequenceNumber;
	private volatile long sequenceNumberToolAccess;
	// remote sequences
	private final Map<IndividualAddress, Long> lastValidSequence = new ConcurrentHashMap<>();
	private final Map<IndividualAddress, Long> lastValidSequenceToolAccess = new ConcurrentHashMap<>();

	private final Map<GroupAddress, byte[]> groupKeys;
	private final Map<IndividualAddress, byte[]> toolKeys;
	private final Map<GroupAddress, Set<IndividualAddress>> groupSenders;


	private static final Duration SyncTimeout = Duration.ofSeconds(6);
	private volatile Instant lastSyncRes = Instant.EPOCH;

	// device address -> { challenge, Instant, Future }
	// NYI key w/ serial number for broadcasts
	final Map<IndividualAddress, Object[]> pendingSyncRequests = new ConcurrentHashMap<>();
	private boolean syncReqBroadcast;

	private static final int FunctionPropertyExtCommand = 0b0111010100;
	private static final int FunctionPropertyExtStateResponse = 0b0111010110;
	private static final int GroupObjectTableType = 9;
	private static final int pidGoDiagnostics = 66;

	private final Map<IndividualAddress, CompletableFuture<ReturnCode>> pendingGoDiagnostics = new ConcurrentHashMap<>();


	// logic for security failure counters, they saturate on 16-bit (unsigned short)
	private static final IntUnaryOperator saturatingIncrement = i -> Math.min(i + 1, (1 << 16) - 1);

	private final AtomicInteger scfErrors = new AtomicInteger();
	private final AtomicInteger seqErrors = new AtomicInteger();
	private final AtomicInteger cryptoErrors = new AtomicInteger();
	private final AtomicInteger accessAndRoleErrors = new AtomicInteger();

	private final EventListeners<NetworkLinkListener> listeners = new EventListeners<>();

	private final NetworkLinkListener linkListener = new NetworkLinkListener() {
		@Override
		public void indication(final FrameEvent e) { extract(e).ifPresent(SecureApplicationLayer.this::dispatchLinkEvent); }

		@Override
		public void confirmation(final FrameEvent e) { extract(e).ifPresent(SecureApplicationLayer.this::dispatchLinkEvent); }

		@Override
		public void linkClosed(final CloseEvent e) { listeners.fire(ll -> ll.linkClosed(e)); }
	};


	public final class SalService {
		private final SecurityControl ctrl;
		private final byte[] apdu;

		SalService(final SecurityControl ctrl, final byte[] apdu) {
			this.apdu = apdu;
			this.ctrl = ctrl;
		}

		public final SecurityControl security() { return ctrl; }

		public final byte[] apdu() { return apdu.clone(); }
	}


	public static final boolean isSecuredService(final CEMILData ldata) {
		final byte[] payload = ldata.getPayload();
		final int service = DataUnitBuilder.getAPDUService(payload);
		return service == SecureService;
	}

	public SecureApplicationLayer(final KNXNetworkLink link, final Map<GroupAddress, byte[]> groupKeys,
			final Map<GroupAddress, Set<IndividualAddress>> groupSenders,
			final Map<IndividualAddress, byte[]> deviceToolKeys) {
		this(link, new byte[6], 0, deviceToolKeys, groupKeys, groupSenders);
		link.addLinkListener(linkListener);
	}

	protected SecureApplicationLayer(final KNXNetworkLink link, final byte[] serialNumber, final long sequenceNumber,
			final Map<IndividualAddress, byte[]> deviceToolKeys) {
		this(link, serialNumber, sequenceNumber, deviceToolKeys, Map.of(), Map.of());
	}

	private SecureApplicationLayer(final KNXNetworkLink link, final byte[] serialNumber, final long sequenceNumber,
			final Map<IndividualAddress, byte[]> deviceToolKeys, final Map<GroupAddress, byte[]> groupKeys,
			final Map<GroupAddress, Set<IndividualAddress>> groupSenders) {
		this.link = link;

		if (serialNumber.length != 6)
			throw new KNXIllegalArgumentException("serial number not 6 bytes");
		this.serialNumber = serialNumber.clone();

		this.logger = LogService.getLogger("calimero." + secureSymbol + "-AL " + link.getName());

		this.groupKeys = Map.copyOf(groupKeys);
		this.toolKeys = Map.copyOf(deviceToolKeys);
		this.groupSenders = Map.copyOf(groupSenders);

		this.sequenceNumber = sequenceNumber;
		sequenceNumberToolAccess = 1;
	}

	public void addListener(final NetworkLinkListener l) { listeners.add(l); }

	public void removeListener(final NetworkLinkListener l) { listeners.remove(l); }

	public Optional<byte[]> secureGroupObject(final IndividualAddress src, final GroupAddress dst, final byte[] apdu)
			throws InterruptedException {
		final int flags = groupObjectSecurity(dst);
		final boolean conf = (flags & 2) == 2;
		final boolean auth = (flags & 1) == 1;
		if (!conf && !auth)
			return Optional.empty();
		final boolean toolAccess = sequenceNumber == 0;
		final var security = conf ? DataSecurity.AuthConf : DataSecurity.Auth;
		return secureData(src, dst, apdu, SecurityControl.of(security, toolAccess));
	}

	public CompletableFuture<ReturnCode> writeGroupObjectDiagnostics(final GroupAddress group, final byte[] value)
			throws KNXTimeoutException, KNXLinkClosedException, InterruptedException {
		final int oinstance = 1;

		// write service IDs
//		final int setLocalGOValue = 0;
		final int sendGroupValueWrite = 1;
//		final int sendLocalGOValueOnBus = 2;
		final int sendGroupValueRead = 3;
//		final int limitGroupServiceSenders = 4;
		final int service = value.length == 0 ? sendGroupValueRead : sendGroupValueWrite;

		final boolean conf = true;
		final boolean auth = true;
		final boolean longApdu = value.length == 1 && value[0] < 64 ? false : false;
		final int flags = (longApdu ? 0x80 : 0) | (conf ? 2 : 0) | (auth ? 1 : 0);

		final var asdu = ByteBuffer.allocate(10 + value.length).putShort((short) GroupObjectTableType)
				.put((byte) (oinstance >> 4)).put((byte) (((oinstance & 0xf) << 4) | (pidGoDiagnostics >> 8)))
				.put((byte) pidGoDiagnostics).put((byte) 0).put((byte) service).put((byte) flags)
				.put(group.toByteArray()).put(value);
		final var apdu = DataUnitBuilder.createAPDU(FunctionPropertyExtCommand, asdu.array());

		final var surrogate = surrogate(group);
		final var secCtrl = SecurityControl.of(DataSecurity.AuthConf, true);
		final var secureApdu = secureData(address(), surrogate, apdu, secCtrl).get();
		logger.trace("{}->{} GO diagnostics {} {}", address(), surrogate, service, DataUnitBuilder.toHex(value, " "));
		send(surrogate, secureApdu);

		final var future = new CompletableFuture<ReturnCode>().orTimeout(3, TimeUnit.SECONDS);
		pendingGoDiagnostics.put(surrogate, future);
		return future.whenComplete((__, ___) -> pendingGoDiagnostics.remove(surrogate));
	}

	private void checkGoDiagnosticsResponse(final IndividualAddress src, final IndividualAddress dst, final int service,
			final byte[] apdu) {
		if (service != FunctionPropertyExtStateResponse || apdu.length < 9)
			return;
		final var data = ByteBuffer.wrap(apdu, 2, apdu.length - 2);
		final int ot = data.getShort() & 0xffff;
		if (ot != GroupObjectTableType)
			return;
		final int i = data.getShort() & 0xffff;
		final int oinstance = (i >> 4) & 0xfff;
		if (oinstance != 1)
			return;
		final int pid = (i & 0xf) << 8 | data.get() & 0xff;
		if (pid != pidGoDiagnostics)
			return;
		final var returnCode = ReturnCode.of(data.get() & 0xff);
		final int goService = data.get() & 0xff;
		logger.trace("{}->{} GO diagnostics {} {}", src, dst, goService, returnCode);

		final var future = pendingGoDiagnostics.get(src);
		if (future != null)
			future.complete(returnCode);
	}

	public Optional<byte[]> secureData(final IndividualAddress src, final KNXAddress dst, final byte[] apdu,
			final SecurityControl securityCtrl) throws InterruptedException {
		if (securityCtrl == SecurityControl.Plain)
			return Optional.of(apdu);

		final boolean toolAccess = securityCtrl.toolAccess();
		final byte[] key = toolAccess ? toolKey((IndividualAddress) dst) : securityKey(dst);
		if (key == null)
			return Optional.empty();

		final long seqTool = nextSequenceNumber(toolAccess);
		if (seqTool <= 1)
			syncWith(dst, toolAccess);

		final var sapdu = secure(SecureDataPdu, src, dst, apdu, securityCtrl);
		updateSequenceNumber(toolAccess, nextSequenceNumber(toolAccess) + 1);
		return sapdu;
	}

	Optional<byte[]> secure(final int service, final IndividualAddress src, final KNXAddress dst, final byte[] apdu,
			final SecurityControl secCtrl) {
		final boolean toolAccess = secCtrl.toolAccess();
		if (toolAccess) {
			if (secCtrl.security() != DataSecurity.AuthConf)
				throw new KNXIllegalArgumentException("tool access requires auth+conf security");
			if (dst instanceof GroupAddress && dst.getRawAddress() != 0)
				throw new KNXIllegalArgumentException("tool access requires individual address");
		}

		final byte[] key = toolAccess ? toolKey(syncReqBroadcast ? address() : (IndividualAddress) dst) : securityKey(dst);
		if (key == null)
			return Optional.empty();

		final boolean syncReq = service == SecureSyncRequest;
		final boolean syncRes = service == SecureSyncResponse;
		final int snoLength = syncReq ? 6 : 0;
		final ByteBuffer secureApdu = ByteBuffer.allocate(3 + SeqSize + snoLength + apdu.length + MacSize);

		final int tpci = tpci(dst) | (SecureService >> 8);
		secureApdu.put((byte) tpci);
		secureApdu.put((byte) SecureService);

		// NYI system broadcast option
		final boolean systemBcast = false;

		final int scf = toSecurityCtrlField(service, secCtrl, systemBcast);
		secureApdu.put((byte) scf);

		final long seqSend = nextSequenceNumber(toolAccess);
		if (seqSend == 0)
			throw new KnxSecureException("0 is not a valid sequence number");
		logger.trace("use {}sequence {}", toolAccess ? "tool access " : "", seqSend);
		final ByteBuffer seq = sixBytes(seqSend);
		if (!syncRes)
			secureApdu.put(seq);

		final var associatedData = ByteBuffer.allocate(syncReq ? 7 : 1).put((byte) scf);
		final byte[] seqOrRand = seqOrRand(service, seq.array());
		if (syncReq) {
			// NYI lookup serial number of target device for SBC sync.req
			final var remoteSerialNo = new byte[6];
			secureApdu.put(systemBcast ? remoteSerialNo : new byte[6]);
			associatedData.put(serialNumber);
		}
		else if (syncRes) {
			final var request = pendingSyncRequests.remove(dst);
			if (request == null)
				throw new KnxSecureException("sending sync.res without corresponding .req");
			final BitSet rndXorChallenge = BitSet.valueOf(seqOrRand);
			final ByteBuffer challenge = sixBytes((long) request[0]);
			rndXorChallenge.xor(BitSet.valueOf(challenge));
			secureApdu.put(rndXorChallenge.toByteArray());
		}

		final int extendedFrameFormat = 0;
		final byte[] iv = block0(seqOrRand, src, dst, extendedFrameFormat, tpci, SecureService, apdu.length);
		final var ctr0 = blockCtr0(seqOrRand, src, dst);

		try {
			if (secCtrl.security() == DataSecurity.AuthConf) {
				final byte[] mac = confMac(associatedData.array(), apdu, key, iv);
				final byte[] input = ByteBuffer.allocate(MacSize + apdu.length).put(mac).put(apdu).array();
				final byte[] encrypted = encrypt(input, key, ctr0);
				secureApdu.put(encrypted, MacSize, apdu.length);
				secureApdu.put(encrypted, 0, MacSize);
			}
			else {
				secureApdu.put(apdu);
				final byte[] mac = mac(apdu, key, iv, ctr0);
				secureApdu.put(mac);
			}
		}
		catch (final GeneralSecurityException e) {
			securityFailure(CryptoError, src, dst, seqSend);
			throw new KnxSecureException(format("securing %s->%s", src, dst), e);
		}

		return Optional.of(secureApdu.array());
	}

	public SalService extract(final CEMILData ldata) {
		final byte[] payload = ldata.getPayload();
		if (payload.length < 2)
			return new SalService(SecurityControl.Plain, payload);
		final int service = DataUnitBuilder.getAPDUService(payload);
		if (service != SecureService)
			return new SalService(SecurityControl.Plain, payload);
		// TPCI + APCI + SCF + seq + 1 byte APDU + MAC
		if (payload.length < 14) {
			securityFailure(CryptoError, ldata.getSource(), ldata.getDestination(), 0);
			throw new KnxSecureException("frame length " + payload.length + " too short for a secure frame");
		}
		return extract(ldata.getSource(), ldata.getDestination(), payload);
	}

	SalService extract(final IndividualAddress src, final KNXAddress dst, final byte[] secureApdu) {
		final int service = DataUnitBuilder.getAPDUService(secureApdu);
		if (service != SecureService)
			throw new KNXIllegalArgumentException(
					format("%s is not a secure service", DataUnitBuilder.decodeAPCI(service)));

		final int tpci = secureApdu[0] & 0xff;
		final var secureAsdu = DataUnitBuilder.extractASDU(secureApdu);
		return decrypt(src, dst, tpci, secureAsdu);
	}

	protected Optional<FrameEvent> extract(final FrameEvent e) {
		final var cemi = e.getFrame();
		if (cemi instanceof CEMILData) {
			final CEMILData ldata = (CEMILData) cemi;
			try {
				final var salData = extract(ldata);
				if (salData.apdu().length == 0)
					return Optional.empty();
				if (salData.security() == SecurityControl.Plain)
					return Optional.of(e);

				final var plain = CEMIFactory.create(cemi.getMessageCode(), salData.apdu(), cemi);
				final var extracted = new FrameEvent(e.getSource(), plain, e.systemBroadcast(),
						salData.security());
				return Optional.of(extracted);
			}
			catch (final KnxSecureException kse) {
				logger.info(kse.toString());
			}
			catch (KNXFormatException | RuntimeException ex) {
				logger.warn(ex.toString());
			}
			return Optional.empty();
		}
		return Optional.of(e);
	}

	public SalService decrypt(final IndividualAddress src, final KNXAddress dst, final int tpci, final byte[] secureAsdu) {
		final ByteBuffer asdu = ByteBuffer.wrap(secureAsdu, 0, secureAsdu.length);
		final int scf = asdu.get() & 0xff;
		final Object[] flags = parseSecurityCtrlField(scf, src, dst, 0);
		final var securityCtrl = (SecurityControl) flags[0];
		final boolean toolAccess = securityCtrl.toolAccess();
		final boolean systemBcast = (Boolean) flags[1];
		final int service = (Integer) flags[2];

		final boolean syncReq = service == SecureSyncRequest;
		final boolean syncRes = service == SecureSyncResponse;

		final boolean isGroupDst = dst instanceof GroupAddress;
		// if we have a group service, check group key table first
		final var key = isGroupDst ? securityKey(dst)
				: toolAccess ? toolKey(src.equals(address()) ? (IndividualAddress) dst : src) : securityKey(src);
		if (key == null)
			return new SalService(securityCtrl, new byte[0]);

		byte[] seq = new byte[6];
		asdu.get(seq);
		final long receivedSeq = toLong(seq);

		// using tool access for group service where a group key is available is considered an attack
		if (isGroupDst && toolAccess) {
			securityFailure(AccessAndRoleError, src, dst, receivedSeq);
			throw new KnxSecureException(format("%s->%s group service with tool access", src, dst));
		}

		final byte[] sno = new byte[6];
		if (service == SecureDataPdu) {
			final long expectedSeq = lastValidSequenceNumber(toolAccess, src) + 1;
			if (receivedSeq < expectedSeq) {
				securityFailure(SeqNoError, src, dst, receivedSeq);
				throw new KnxSecureException(format("%s->%s received sequence number %d < %d (expected)", src, dst,
						receivedSeq, expectedSeq));
			}
		}
		else if (syncReq) {
			asdu.get(sno);
			// ignore sync.reqs not addressed to us
			if (!Arrays.equals(sno, serialNumber)) {
				if (systemBcast || !dst.equals(address()) || !Arrays.equals(sno, new byte[6]))
					return new SalService(securityCtrl, new byte[0]);
			}
			// if responded to another request within the last 1 second, ignore
			if (Instant.now().minusSeconds(1).isBefore(lastSyncRes))
				return new SalService(securityCtrl, new byte[0]);
		}
		else if (syncRes) {
			final var request = pendingSyncRequests.get(src);
			if (request == null)
				return new SalService(securityCtrl, new byte[0]);

			// in a sync.res, seq actually contains our challenge from sync.req xored with a random value
			// extract the random value and store it in seq to use it for block0 and ctr0
			final var challengeXorRandom = BitSet.valueOf(seq);
			final var challenge = BitSet.valueOf(sixBytes((long) request[0]));
			challengeXorRandom.xor(challenge);
			seq = challengeXorRandom.toByteArray();
		}

		final var s = service == SecureSyncRequest ? "sync.req"
				: service == SecureSyncResponse ? "sync.res" : "S-A_Data";
		logger.debug("{}->{} decrypt {} ({})", src, dst, s, securityCtrl);

		final byte[] apdu = new byte[asdu.remaining() - MacSize];
		asdu.get(apdu);

		final var ctr0 = blockCtr0(seq, src, dst);

		if (securityCtrl.security() == DataSecurity.Auth) {

		}
		else {
			final var mac = new byte[MacSize];
			asdu.get(mac);
			final var input = ByteBuffer.allocate(MacSize + apdu.length).put(mac).put(apdu);
			final byte[] decrypted;
			try {
				decrypted = decrypt(input.array(), key, ctr0);
			}
			catch (final GeneralSecurityException e) {
				securityFailure(CryptoError, src, dst, receivedSeq);
				throw new KnxSecureException(format("decrypting %s->%s", src, dst), e);
			}

			final var decryptedMac = Arrays.copyOfRange(decrypted, 0, MacSize);
			final var plainApdu = Arrays.copyOfRange(decrypted, MacSize, decrypted.length);
			final int extendedFrameFormat = 0;
			final byte[] iv = block0(seq, src, dst, extendedFrameFormat, tpci, SecureService, plainApdu.length);
			try {
				final var associatedData = ByteBuffer.allocate(syncReq ? 7 : 1).put((byte) scf);
				if (syncReq)
					associatedData.put(sno);

				final byte[] calculated = confMac(associatedData.array(), plainApdu, key, iv);
				if (!Arrays.equals(calculated, decryptedMac)) {
					securityFailure(CryptoError, src, dst, receivedSeq);
					throw new KnxSecureException(format("MAC mismatch %s->%s", src, dst));
				}
			}
			catch (final GeneralSecurityException e) {
				securityFailure(CryptoError, src, dst, receivedSeq);
				throw new KnxSecureException(format("calculating MAC %s->%s", src, dst), e);
			}

			// prevent a sync.req sent by us to trigger sync notification, this happens if we provide our own tool key
			// for decryption above
			if (syncReq && src.equals(address()))
				return new SalService(securityCtrl, new byte[0]);

			if (syncReq) {
				receivedSyncRequest(src, dst, toolAccess, seq, toLong(plainApdu));
				return new SalService(securityCtrl, new byte[0]);
			}

			if (syncRes) {
				receivedSyncResponse(src, toolAccess, plainApdu);
				return new SalService(securityCtrl, new byte[0]);
			}

			if (src.equals(address())) {
				logger.trace("update next {}seq -> {}", toolAccess ? "tool access " : "", receivedSeq);
				updateSequenceNumber(toolAccess, receivedSeq + 1);
			}
			else {
				logger.trace("update last valid {}seq of {} -> {}", toolAccess ? "tool access " : "", src, receivedSeq);
				updateLastValidSequence(toolAccess, src, receivedSeq);
			}

			final int plainService = DataUnitBuilder.getAPDUService(plainApdu);
			if (!isGroupDst)
				checkGoDiagnosticsResponse(src, (IndividualAddress) dst, plainService, plainApdu);

			if (!checkAccess(dst, plainService, securityCtrl)) {
				securityFailure(AccessAndRoleError, src, dst, receivedSeq);
				throw new KnxSecureException(format("%s->%s denied access for %s (%s)", src, dst,
						DataUnitBuilder.decodeAPCI(plainService), securityCtrl));
			}
			return new SalService(securityCtrl, plainApdu);
		}
		return new SalService(securityCtrl, apdu);
	}

	public CompletableFuture<Void> sendSyncRequest(final IndividualAddress remote, final boolean toolAccess)
			throws KNXTimeoutException, KNXLinkClosedException {
		final var challenge = ThreadLocalRandom.current().nextLong();
		final byte[] secureApdu = secure(SecureSyncRequest, address(), remote, sixBytes(challenge).array(),
				SecurityControl.of(DataSecurity.AuthConf, toolAccess)).get();
		logger.debug("sync {} seq with {}", toolAccess ? "tool access" : "p2p", remote);
		send(remote, secureApdu);
		return stashSyncRequest(remote, challenge);
	}

	@Override
	public void close() {
		link.removeLinkListener(linkListener);
	}

	protected void dispatchLinkEvent(final FrameEvent e) {
		final var cemi = e.getFrame();
		if (cemi.getMessageCode() == CEMILData.MC_LDATA_IND)
			listeners.fire(ll -> ll.indication(e));
		else if (cemi.getMessageCode() == CEMILData.MC_LDATA_CON)
			listeners.fire(ll -> ll.confirmation(e));
	}

	protected byte[] toolKey(final IndividualAddress device) { return toolKeys.get(device); }

	protected byte[] securityKey(final KNXAddress addr) {
		if (addr instanceof GroupAddress) {
			final var group = (GroupAddress) addr;
			final var key = groupKeys.get(group);
			if (key == null)
				throw new KnxSecureException("no group key for " + group);
			return key;
		}
		return null;
	}

	// returns next outgoing sequence number for secure communication
	long nextSequenceNumber(final boolean toolAccess) {
		return toolAccess ? sequenceNumberToolAccess : sequenceNumber;
	}

	// stores next outgoing sequence number for secure communication
	protected void updateSequenceNumber(final boolean toolAccess, final long seqNo) {
		if (toolAccess)
			sequenceNumberToolAccess = seqNo;
		else
			sequenceNumber = seqNo;
	}

	protected long lastValidSequenceNumber(final boolean toolAccess, final IndividualAddress remote) {
		if (toolAccess)
			return lastValidSequenceToolAccess.getOrDefault(remote, 0L);
		return lastValidSequence.getOrDefault(remote, 0L);
	}

	protected void updateLastValidSequence(final boolean toolAccess, final IndividualAddress remote, final long seqNo) {
		if (toolAccess)
			lastValidSequenceToolAccess.put(remote, seqNo);
		else
			lastValidSequence.put(remote, seqNo);
	}

	protected boolean checkAccess(final KNXAddress dst, final int service, final SecurityControl securityCtrl) {
		return true;
	}

	protected int groupObjectSecurity(final GroupAddress group) {
		if (groupKeys.containsKey(group))
			return 3;
		return 0;
	}

	protected int tpci(final KNXAddress dst) { return 0; }

	// implicitly system priority
	protected void send(final KNXAddress remote, final byte[] secureApdu)
			throws KNXTimeoutException, KNXLinkClosedException {
		link.sendRequestWait(remote, Priority.SYSTEM, secureApdu);
	}

	protected final int failureCounter(final int errorType) {
		switch (errorType) {
			case InvalidScf: return scfErrors.get();
			case SeqNoError: return seqErrors.get();
			case CryptoError: return cryptoErrors.get();
			case AccessAndRoleError: return accessAndRoleErrors.get();
			default: throw new IllegalArgumentException("failure counter error type " + errorType);
		}
	}

	protected void securityFailure(final int errorType, final IntUnaryOperator updateFunction,
			final IndividualAddress src, final KNXAddress dst, final int ctrlExtended, final long seqNo) {
		final AtomicInteger[] counters = { null, scfErrors, seqErrors, cryptoErrors, accessAndRoleErrors };
		if (errorType > 4)
			throw new IllegalArgumentException("failure counter error type " + errorType);
		counters[errorType].updateAndGet(updateFunction);
	}

	private void securityFailure(final int errorType, final IndividualAddress src, final KNXAddress dst,
			final long seqNo) {
		final int ctrlExtended = dst instanceof GroupAddress ? 0x80 : 0x0;
		securityFailure(errorType, saturatingIncrement, src, dst, ctrlExtended, seqNo);
	}

	void receivedSyncRequest(final IndividualAddress src, final KNXAddress dst, final boolean toolAccess, final byte[] seq,
			final long challenge) {
		final long nextRemoteSeq = toLong(seq);
		long nextSeq = 1 + lastValidSequenceNumber(toolAccess, src);
		final String tool = toolAccess ? "tool " : "";
		if (nextRemoteSeq > nextSeq) {
			updateLastValidSequence(toolAccess, src, nextRemoteSeq - 1);
			nextSeq = nextRemoteSeq;
		}
		logger.debug("{}->{} sync.req with {}seq {} (next {}), challenge {}", src, dst, tool, nextRemoteSeq, nextSeq,
				challenge);
		// the returned completable future here is not important
		stashSyncRequest(src, challenge);
		syncReqBroadcast = dst.equals(new GroupAddress(0));
		final var to = syncReqBroadcast ? dst : src;
		try {
			sendSyncResponse(to, toolAccess, nextSeq);
		}
		catch (final KNXException e) {
			logger.warn("error sending sync.res {}->{}", address(), to, e);
		}
	}

	void receivedSyncResponse(final IndividualAddress remote, final boolean toolAccess, final byte[] plainApdu) {
		final var request = pendingSyncRequests.get(remote);
		if (request == null)
			return;

		final var remoteSeq = toLong(Arrays.copyOfRange(plainApdu, 0, SeqSize));
		final var localSeq = toLong(Arrays.copyOfRange(plainApdu, SeqSize, SeqSize + SeqSize));

		final long last = lastValidSequenceNumber(toolAccess, remote);
		if (remoteSeq - 1 > last) {
			logger.debug("sync.res update {} last valid {} seq -> {}", remote, toolAccess ? "tool access" : "p2p", remoteSeq -1);
			updateLastValidSequence(toolAccess, remote, remoteSeq - 1);
		}

		final long next = nextSequenceNumber(toolAccess);
		if (localSeq > next) {
			logger.debug("sync.res update local next {} seq -> {}", toolAccess ? "tool access" : "p2p", localSeq);
			updateSequenceNumber(toolAccess, localSeq);
		}
		syncRequestCompleted(request);
	}

	private void sendSyncResponse(final KNXAddress dst, final boolean toolAccess, final long remoteNextSeq)
			throws KNXTimeoutException, KNXLinkClosedException {
		final var ourNextSeq = nextSequenceNumber(toolAccess);
		final var asdu = ByteBuffer.allocate(12).put(sixBytes(ourNextSeq)).put(sixBytes(remoteNextSeq));
		final var response = secure(SecureSyncResponse, address(), dst, asdu.array(),
				SecurityControl.of(DataSecurity.AuthConf, toolAccess)).get();

		lastSyncRes = Instant.now();
		send(dst, response);
	}

	CompletableFuture<Void> stashSyncRequest(final IndividualAddress addr, final long challenge) {
		final var future = new CompletableFuture<Void>().orTimeout(SyncTimeout.toSeconds(), TimeUnit.SECONDS);
		pendingSyncRequests.put(addr, new Object[] { challenge, Instant.now(), future });
		return future.whenComplete((__, ___) -> pendingSyncRequests.remove(addr));
	}

	@SuppressWarnings("unchecked")
	private void syncRequestCompleted(final Object[] request) {
		((CompletableFuture<Void>) request[2]).complete(null);
	}

	private void syncWith(final KNXAddress dst, final boolean toolAccess) throws InterruptedException {
		try {
			final var device = dst instanceof GroupAddress ? surrogate((GroupAddress) dst) : (IndividualAddress) dst;
			final var future = sendSyncRequest(device, toolAccess);
			future.get();
		}
		catch (final KNXException e) {
			throw new KnxSecureException("sync.req with " + dst, e);
		}
		catch (final ExecutionException e) {
			throw new KnxSecureException("sync.req with " + dst, e.getCause());
		}
	}

	private IndividualAddress surrogate(final GroupAddress group) {
		final var surrogate = groupSenders.getOrDefault(group, Set.of()).stream().findAny()
				.orElseThrow(() -> new KnxSecureException(group + " does not have a surrogate specified"));
		return surrogate;
	}

	private IndividualAddress address() {
		return link.getKNXMedium().getDeviceAddress();
	}

	private Object[] parseSecurityCtrlField(final int scf, final IndividualAddress src, final KNXAddress dst,
			final long receivedSeq) {
		final boolean toolAccess = (scf & 128) == 128;
		final int algorithmId = (scf >> 4) & 0x7;
		if (algorithmId > 1) {
			securityFailure(InvalidScf, src, dst, receivedSeq);
			throw new KnxSecureException("unsupported secure algorithm ID " + algorithmId);
		}
		final boolean authOnly = algorithmId == 0;
		final boolean systemBroadcast = (scf & 0x8) == 0x8;
		final int service = scf & 0x7;
		if (service == 1 || service > 3) {
			securityFailure(InvalidScf, src, dst, receivedSeq);
			throw new KnxSecureException("unsupported secure AL service " + service);
		}
		final var ctrl = SecurityControl.of(authOnly ? DataSecurity.Auth : DataSecurity.AuthConf, toolAccess);
		return new Object[] { ctrl, systemBroadcast, service };
	}

	private static int toSecurityCtrlField(final int service, final SecurityControl secCtrl, final boolean systemBcast) {
		int scf = service;
		scf |= secCtrl.toolAccess() ? 0x80 : 0;
		scf |= secCtrl.security() == DataSecurity.AuthConf ? 0x10 : 0;
		scf |= systemBcast ? 0x8 : 0;
		return scf;
	}

	private static ByteBuffer sixBytes(final long num) {
		return ByteBuffer.allocate(6).putShort((short) (num >> 32)).putInt((int) num).flip();
	}

	private static long toLong(final byte[] data) {
		long l = 0;
		for (final byte b : data)
			l = (l << 8) + (b & 0xff);
		return l;
	}

	private static final SecureRandom rng = new SecureRandom();

	private static byte[] seqOrRand(final int service, final byte[] seq) {
		if (service == SecureSyncResponse) {
			if (test)
				return new byte[] { (byte) 0xaa, (byte) 0xaa, (byte) 0xaa, (byte) 0xaa, (byte) 0xaa, (byte) 0xaa };
			rng.nextBytes(seq);
		}
		return seq;
	}

	private static byte[] mac(final byte[] apdu, final byte[] key, final byte[] iv, final byte[] ctr0)
			throws GeneralSecurityException {
		final ByteBuffer buf = ByteBuffer.allocate(2 + apdu.length);
		buf.putShort((short) apdu.length);
		buf.put(apdu);
		final byte[] y = aesCbc(buf.array(), key, iv);
		final byte[] msbY = Arrays.copyOfRange(y, 0, MacSize);
		final byte[] result = encrypt(msbY, key, ctr0);
		return Arrays.copyOfRange(result, 0, MacSize);
	}

	private static byte[] confMac(final byte[] associatedData, final byte[] apdu, final byte[] key, final byte[] iv)
			throws GeneralSecurityException {
		final ByteBuffer buf = ByteBuffer.allocate(2 + associatedData.length + apdu.length);
		buf.putShort((short) associatedData.length);
		buf.put(associatedData);
		buf.put(apdu);
		final var y = aesCbc(buf.array(), key, iv);
		return Arrays.copyOfRange(y, y.length - 16, y.length - 16 + MacSize);
	}

	private static byte[] aesCbc(final byte[] input, final byte[] key, final byte[] iv)
			throws GeneralSecurityException {
		final var cipher = Cipher.getInstance("AES/CBC/NoPadding");
		final var secretKey = new SecretKeySpec(key, "AES");
		final var params = new IvParameterSpec(new byte[16]);

		cipher.init(Cipher.ENCRYPT_MODE, secretKey, params);
		cipher.update(iv);
		final byte[] padded = Arrays.copyOf(input, (input.length + 15) / 16 * 16);
		return cipher.doFinal(padded);
	}

	private static byte[] block0(final byte[] seqOrRand, final IndividualAddress src, final KNXAddress dst,
			final int extendedFrameFormat, final int tpci, final int apci, final int payloadLength) {
		return ccmBlock(true, seqOrRand, src, dst, extendedFrameFormat, tpci, apci, payloadLength);
	}

	private static byte[] blockCtr0(final byte[] seqOrRand, final IndividualAddress src, final KNXAddress dst) {
		return ccmBlock(false, seqOrRand, src, dst, 0, 0, 0, 0);
	}

	// b0: B0 or block counter 0
	private static byte[] ccmBlock(final boolean b0, final byte[] seqOrRand, final IndividualAddress src,
			final KNXAddress dst, final int extendedFrameFormat, final int tpci, final int apci, final int payloadLength) {
		final ByteBuffer block = ByteBuffer.allocate(16);
		block.put(seqOrRand);
		block.put(src.toByteArray());
		block.put(dst.toByteArray());
		if (b0) {
			block.put((byte) 0);
			final boolean group = dst instanceof GroupAddress;
			final int at = (group ? 0x80 : 0) | (extendedFrameFormat & 0xf);
			block.put((byte) at);
			block.put((byte) tpci);
			block.put((byte) apci);
			block.put((byte) 0);
			block.put((byte) payloadLength);
		}
		else {
			block.putInt(0);
			block.put((byte) 1);
		}
		return block.array();
	}

	private static byte[] encrypt(final byte[] input, final byte[] key, final byte[] iv)
			throws GeneralSecurityException {
		final var cipher = Cipher.getInstance("AES/CTR/NoPadding");
		final var secretKey = new SecretKeySpec(key, "AES");
		final var params = new IvParameterSpec(iv);

		cipher.init(Cipher.ENCRYPT_MODE, secretKey, params);
		final byte[] padded = Arrays.copyOf(input, (input.length + 15) / 16 * 16);
		return cipher.doFinal(padded);
	}

	private static byte[] decrypt(final byte[] input, final byte[] key, final byte[] iv)
			throws GeneralSecurityException {
		final var cipher = Cipher.getInstance("AES/CTR/NoPadding");
		final var secretKey = new SecretKeySpec(key, "AES");
		final var params = new IvParameterSpec(iv);

		cipher.init(Cipher.DECRYPT_MODE, secretKey, params);
		return cipher.doFinal(input);
	}
}
