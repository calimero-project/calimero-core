/*
    Calimero 2 - A library for KNX network access
    Copyright (c) 2015, 2021 B. Malinowsky

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

package tuwien.auto.calimero.dptxlator;

import java.util.HashMap;
import java.util.Map;

import tuwien.auto.calimero.KNXFormatException;

/**
 * Translator for KNX DPTs with main number 20, type <b>8 Bit Enumeration (N8)</b>. It provides the
 * enumerations for the following application domains:
 * <ul>
 * <li>Generic</li>
 * <li>HVAC</li>
 * <li>Lighting</li>
 * <li>Shutter/Blinds</li>
 * </ul>
 * The KNX data type width is 1 byte. The default return value after creation is the minimum element
 * specified in the ordered listing of the set of elements.
 * <p>
 * In value methods expecting string items, the item might be formatted using decimal, hexadecimal,
 * and octal numbers, distinguished by using the following prefixes:
 * <dl>
 * <dt>no prefix</dt>
 * <dd>for decimal numeral</dd>
 * <dt><code>0x</code>, <code>0X</code>, <code>#</code>
 * <dd>for hexadecimal numeral</dd>
 * <dt><code>0</code>
 * <dd>for octal numeral</dd>
 * </dl>
 */
@SuppressWarnings("checkstyle:javadocvariable")
public class DPTXlator8BitEnum extends DPTXlator
{
	private interface EnumBase<E extends Enum<E> & EnumBase<E>> extends EnumDptBase.EnumBase<E> {
		Map<EnumBase<?>, Integer> values = new HashMap<>();
		Map<EnumBase<?>, String> descriptions = new HashMap<>();

		default void init(final int element) { init(element, split(this.toString())); }

		default void init(final int element, final String description) {
			values.put(this, element);
			descriptions.put(this, description);
		}

		@Override
		default int value() { return values.get(this); }

		@Override
		default String description() { return descriptions.get(this); }

		private static String split(final String name) { return name.replaceAll("\\B([A-Z])", " $1").toLowerCase(); }
	}


	// Non-standard enum HVAC Command field (Z8): 8 bit enumeration value in case of a HVAC property/group-property write service
	enum HvacCommand implements EnumBase<HvacCommand> {
		NormalWrite(0), Override(1), Release(2), SetOutOfService(3), ResetOutOfService(4), AlarmAck(5), SetToDefault(6);

		HvacCommand(final int element) { init(element); }
	}

	// Non-standard enum 'Enable heating/cooling stage' used by DPT 201.105
	enum EnableHeatCoolStage implements EnumBase<EnableHeatCoolStage> {
		Disabled(0),
		EnableStageA(1),
		EnableStageB(2),
		EnableBothStages(3);

		EnableHeatCoolStage(final int element) { init(element); }
	}


	//
	// The specified enumerations for the DPTs of this translator
	//

	public enum SystemClockMode implements EnumBase<SystemClockMode> {
		Autonomous(0),
		Slave(1),
		Master(2);

		SystemClockMode(final int element) { init(element); }
	}

	public enum BuildingMode implements EnumBase<BuildingMode> {
		BuildingInUse(0),
		BuildingNotUsed(1),
		BuildingProtection(2);

		BuildingMode(final int element) { init(element); }
	}

	public enum OccupancyMode implements EnumBase<OccupancyMode> {
		Occupied(0),
		Standby(1),
		NotOccupied(2);

		OccupancyMode(final int element) { init(element); }
	}

	public enum Priority implements EnumBase<Priority> {
		High(0),
		Medium(1),
		Low(2),
		Void(3);

		Priority(final int element) { init(element); }
	}

	public enum LightApplicationMode implements EnumBase<LightApplicationMode> {
		Normal(0),
		PresenceSimulation(1),
		NightRound(2);

		LightApplicationMode(final int element) { init(element); }
	}

	public enum ApplicationArea implements EnumBase<ApplicationArea> {
		NoFault(0, "no fault"),
		CommonInterest(1, "system and functions of common interest"),
		HvacGeneralFBs(10, "HVAC general FBs"),
		HvacHotWaterHeating(11, "HVAC Hot Water Heating"),
		HvacDirectElectricalHeating(12, "HVAC Direct Electrical Heating"),
		HvacTerminalUnits(13, "HVAC Terminal Units"),
		HvacVac(14, "HVAC VAC"), // Ventilation and Air Conditioning
		Lighting(20, "Lighting"),
		Security(30, "Security"),
		LoadManagement(40, "Load Management"),
		ShuttersAndBlinds(50, "Shutters and blinds");

		ApplicationArea(final int element, final String description) { init(element, description); }
	}

	public enum AlarmClassType implements EnumBase<AlarmClassType> {
		SimpleAlarm(1),
		BasicAlarm(2),
		ExtendedAlarm(3);

		AlarmClassType(final int element) { init(element); }
	}

	public enum PsuMode implements EnumBase<PsuMode> {
		Disabled(0, "disabled (PSU/DPSU fixed off)"),
		Enabled(1, "enabled (PSU/DPSU fixed on)"),
		Auto(2, "auto (PSU/DPSU automatic on/off)");

		PsuMode(final int element, final String description) { init(element, description); }
	}

	public enum ErrorClassSystem implements EnumBase<ErrorClassSystem> {
		NoFault(0, "no fault"),
		GeneralDeviceFault(1, "general device fault (e.g., RAM, EEPROM, UI, watchdog)"),
		CommunicationFault(2, "communication fault"),
		ConfigurationFault(3, "configuration fault"),
		HardwareFault(4, "hardware fault"),
		SoftwareFault(5, "software fault"),
		InsufficientNVMemory(6, "insufficient non volatile memory"),
		InsufficientMemory(7, "insufficient volatile memory"),
		MemoryAllocationZero(8, "memory allocation command with size 0 received"),
		CRC(9, "CRC error"),
		WatchdogReset(10, "watchdog reset detected"),
		InvalidOpcode(11, "invalid opcode detected"),
		GeneralProtectionFault(12, "general protection fault"),
		MaxTableLengthExceeded(13, "maximal table length exceeded"),
		UndefinedLoadCommand(14, "undefined load command  received"),
		GroupAddressTableNotSorted(15, "Group Address Table is not sorted"),
		InvalidConnectionNumber(16, "invalid connection number (TSAP)"),
		InvalidGroupObject(17, "invalid Group Object number (ASAP)"),
		GroupObjectTypeExceedsLength(18, "Group Object Type exceeds (PID_MAX_APDU_LENGTH – 2)");

		ErrorClassSystem(final int element, final String description) { init(element, description); }
	}

	public enum ErrorClassHvac implements EnumBase<ErrorClassHvac> {
		NoFault(0, "no fault"),
		SensorFault(1, "sensor fault"),
		ProcessFault(2, "process fault / controller fault"),
		ActuatorFault(3, "actuator fault"),
		OtherFault(4, "other fault");

		ErrorClassHvac(final int element, final String description) { init(element, description); }
	}

	public enum TimeDelay implements EnumBase<TimeDelay> {
		NotActive(0, "not active"),
		Delay1s(1, "1 s"),
		Delay2s(2, "2 s"),
		Delay3s(3, "3 s"),
		Delay5s(4, "5 s"),
		Delay10s(5, "10 s"),
		Delay15s(6, "15 s"),
		Delay20s(7, "20 s"),
		Delay30s(8, "30 s"),
		Delay45s(9, "45 s"),
		Delay1min(10, "1 min"),
		Delay1_25min(11, "1,25 min"),
		Delay1_5min(12, "1,5 min"),
		Delay2min(13, "2 min"),
		Delay2_5min(14, "2,5 min"),
		Delay3min(15, "3 min"),
		Delay5min(16, "5 min"),
		Delay15min(17, "15 min"),
		Delay20min(18, "20 min"),
		Delay30min(19, "30 min"),
		Delay1h(20, "1 h"),
		Delay2h(21, "2 h"),
		Delay3h(22, "3 h"),
		Delay5h(23, "5 h"),
		Delay12h(24, "12 h"),
		Delay24h(25, "24 h");

		TimeDelay(final int element, final String description) { init(element, description); }
	}

	public enum BeaufortWindForceScale implements EnumBase<BeaufortWindForceScale> {
		Calm(0, "calm (< 1.1 km/h)"),
		LightAir(1, "light air (1.1–5.5 km/h)"),
		LightBreeze(2, "light breeze (5.5–11.9 km/h)"),
		GentleBreeze(3, "gentle breeze (11.9–19.7 km/h)"),
		ModerateBreeze(4, "moderate breeze (19.7–28.7 km/h)"),
		FreshBreeze(5, "fresh breeze (28.7–38.8 km/h)"),
		StrongBreeze(6, "strong breeze (38.8–49.9 km/h)"),
		NearGale(7, "near gale, moderate gale (49.9–61.8 km/h)"),
		Gale(8, "gale, fresh gale (61.8–74.6 km/h)"),
		StrongGale(9, "strong gale (74.6–88.1 km/h)"),
		Storm(10, "storm (88.1–102.4 km/h)"),
		ViolentStorm(11, "violent storm (102.4–117.4 km/h)"),
		Hurricane(12, "hurricane (≥ 117.4 km/h)");

		BeaufortWindForceScale(final int element, final String description) { init(element, description); }
	}

	public enum SensorSelect implements EnumBase<SensorSelect> {
		Inactive(0, "inactive"),
		DigitalInput(1, "digital input (not inverted)"),
		DigitalInputInverted(2, "digital input inverted"),
		AnalogInput(3, "analog input, 0 % to 100 %"),
		TemperatureSensorInput(4, "temperature sensor input");

		SensorSelect(final int element, final String description) { init(element, description); }
	}

	public enum ActuatorConnectType implements EnumBase<ActuatorConnectType> {
		SensorConnection(1),
		ControllerConnection(2);

		ActuatorConnectType(final int element) { init(element); }
	}

	public enum CloudCover implements EnumBase<CloudCover> {
		Cloudless(0, "Cloudless"),
		Sunny(1, "Sunny"),
		Sunshiny(2, "Sunshiny"),
		LightlyCloudy(3, "Lightly cloudy"),
		ScatteredClouds(4, "Scattered clouds"),
		Cloudy(5, "Cloudy"),
		SixOktas(6, "6 oktas"),
		SevenOktas(7, "7 oktas"),
		Overcast(8, "Overcast"),
		SkyObstructedFromView(9, "Sky obstructed from view");

		CloudCover(final int element, final String description) { init(element, description); }
	}

	public enum FuelType implements EnumBase<FuelType> {
		Auto(0),
		Oil(1),
		Gas(2),
		SolidStateFuel(3);

		FuelType(final int element) { init(element); }
	}

	public enum BurnerType implements EnumBase<BurnerType> {
		OneStage(1, "1 stage"),
		TwoStage(2, "2 stage"),
		Modulating(3, "modulating");

		BurnerType(final int element, final String description) { init(element, description); }
	}

	public enum HvacMode implements EnumBase<HvacMode> {
		Auto(0),
		Comfort(1),
		Standby(2),
		Economy(3),
		BuildingProtection(4);

		HvacMode(final int element) { init(element); }
	}

	/** Domestic Hot Water Mode. */
	public enum DomesticHotWaterMode implements EnumBase<DomesticHotWaterMode> {
		Auto(0, "Auto"),
		LegioProtect(1, "Legio Protect"),
		Normal(2, "Normal"),
		Reduced(3, "Reduced"),
		OffOrFrostProtect(4, "Off / Frost Protect");

		DomesticHotWaterMode(final int element, final String description) { init(element, description); }
	}

	public enum LoadPriority implements EnumBase<LoadPriority> {
		None(0),
		ShiftLoadPriority(1),
		AbsoluteLoadPriority(2);

		LoadPriority(final int element) { init(element); }
	}

	public enum HvacControlMode implements EnumBase<HvacControlMode> {
		Auto(0, "Auto"),
		Heat(1, "Heat"),
		MorningWarmup(2, "Morning Warmup"),
		Cool(3, "Cool"),
		NightPurge(4, "Night Purge"),
		Precool(5, "Precool"),
		Off(6, "Off"),
		Test(7, "Test"),
		EmergencyHeat(8, "Emergency Heat"),
		FanOnly(9, "Fan Only"),
		FreeCool(10, "Free Cool"),
		Ice(11, "Ice"),
		MaximumHeatingMode(12, "Maximum Heating Mode"),
		EconomicHeatCoolMode(13, "Economic Heat/Cool Mode"),
		Dehumidification(14, "Dehumidification"),
		CalibrationMode(15, "Calibration Mode"),
		EmergencyCoolMode(16, "Emergency Cool Mode"),
		EmergencySteamMode(17, "Emergency Steam Mode"),
		NoDem(20, "NoDem");

		HvacControlMode(final int element, final String description) { init(element, description); }
	}

	public enum HvacEmergencyMode implements EnumBase<HvacEmergencyMode> {
		Normal(0, "Normal"),
		Pressure(1, "Emergency Pressure"),
		Depressure(2, "Emergency Depressure"),
		Purge(3, "Emergency Purge"),
		Shutdown(4, "Emergency Shutdown"),
		Fire(5, "Emergency Fire");

		HvacEmergencyMode(final int element, final String description) { init(element, description); }
	}

	public enum ChangeoverMode implements EnumBase<ChangeoverMode> {
		Auto(0),
		CoolingOnly(1),
		HeatingOnly(2);

		ChangeoverMode(final int element) { init(element); }
	}

	public enum ValveMode implements EnumBase<ValveMode> {
		HeatStageA(1, "Heat stage A for normal heating"),
		HeatStageB(2, "Heat stage B for heating with two stages (A + B)"),
		CoolStageA(3, "Cool stage A for normal cooling"),
		CoolStageB(4, "Cool stage B for cooling with two stages (A + B)"),
		HeatCool(5, "Heat/Cool for changeover applications");

		ValveMode(final int element, final String description) { init(element, description); }
	}

	public enum DamperMode implements EnumBase<DamperMode> {
		FreshAir(1, "Fresh air, e.g., fancoils"),
		SupplyAir(2, "Supply Air, e.g. Variable Air Volume (VAV)"),
		ExtractAir(3, "Extract Air, e.g. Variable Air Volume (VAV)"),
		ExtractAir2(4, "Extract Air, e.g. Variable Air Volume (VAV)");

		DamperMode(final int element, final String description) { init(element, description); }
	}

	public enum HeaterMode implements EnumBase<HeaterMode> {
		HeatStageA(1, "Heat Stage A On/Off"),
		HeatStageAProportional(2, "Heat Stage A Proportional"),
		HeatStageBProportional(3, "Heat Stage B Proportional");

		HeaterMode(final int element, final String description) { init(element, description); }
	}

	public enum FanMode implements EnumBase<FanMode> {
		NotRunning(0, "not running"),
		PermanentlyRunning(1, "permanently running"),
		Intervals(2, "running in intervals");

		FanMode(final int element, final String description) { init(element, description); }
	}

	public enum MasterSlaveMode implements EnumBase<MasterSlaveMode> {
		Autonomous(0),
		Master(1),
		Slave(2);

		MasterSlaveMode(final int element) { init(element); }
	}

	public enum StatusRoomSetpoint implements EnumBase<StatusRoomSetpoint> {
		Normal(0, "normal setpoint"),
		Alternative(1, "alternative setpoint"),
		BuildingProtection(2, "building protection setpoint");

		StatusRoomSetpoint(final int element, final String description) { init(element, description); }
	}

	public enum MeteringDeviceType implements EnumBase<MeteringDeviceType> {
		OtherDeviceType(0, "Other device type"),
		OilMeter(1, "Oil meter"),
		ElectricityMeter(2, "Electricity meter"),
		GasMeter(3, "Gas meter"),
		HeatMeter(4, "Heat meter"),
		SteamMeter(5, "Steam meter"),
		WarmWaterMeter(6, "Warm Water meter"),
		WaterMeter(7, "Water meter"),
		HeatCostAllocator(8, "Heat cost allocator"),
		CoolingLoadMeterOutlet(10, "Cooling Load meter (outlet)"),
		CoolingLoadMeterInlet(11, "Cooling Load meter (inlet)"),
		Heat(12, "Heat (inlet)"),
		HeatAndCool(13, "Heat and Cool"),
		Breaker(32, "breaker (electricity)"),
		Valve(33, "valve (gas or water)"),
		WasteWaterMeter(40, "waste water meter"),
		Garbage(41, "garbage"),
		VoidDeviceType(255, "void device type");

		MeteringDeviceType(final int element, final String description) { init(element, description); }
	}

	public enum AirDamperActuatorType implements EnumBase<AirDamperActuatorType> {

		AirDamper(1),
		VariableAirVolume(2);

		AirDamperActuatorType(final int element) { init(element); }
	}

	public enum BackupMode implements EnumBase<BackupMode> {
		BackupValue(0),
		KeepLastState(1);

		BackupMode(final int element) { init(element); }
	}

	public enum StartSynchronization implements EnumBase<StartSynchronization> {
		PositionUnchanged(0),
		SingleClose(1),
		SingleOpen(2);

		StartSynchronization(final int element) { init(element); }
	}

	public enum BehaviorLockUnlock implements EnumBase<BehaviorLockUnlock> {
		Off(0, "off"),
		On(1, "on"),
		NoChange(2, "no change"),
		AdditionalParameter(3, "value according additional parameter"),
		MemoryFunction(4, "memory function value"),
		UpdatedValue(5, "updated value"),
		ValueBeforeLocking(6, "value before locking");

		BehaviorLockUnlock(final int element, final String description) { init(element, description); }
	}

	public enum BehaviorBusPowerUpDown implements EnumBase<BehaviorBusPowerUpDown> {
		Off(0, "off"),
		On(1, "on"),
		NoChange(2, "no change"),
		AdditionalParameter(3, "value according additional parameter"),
		Last(4, "last (value before bus power down)");

		BehaviorBusPowerUpDown(final int element, final String description) { init(element, description); }
	}

	public enum DaliFadeTime implements EnumBase<DaliFadeTime> {
		NoFade(0, "0 s (no fade)"),
		Fade0_7s(1, "0,7 s"),
		Fade1s(2, "1,0 s"),
		Fade1_4s(3, "1,4 s"),
		Fade2s(4, "2,0 s"),
		Fade2_8s(5, "2,8 s"),
		Fade4s(6, "4,0 s"),
		Fade5_7s(7, "5,7 s"),
		Fade8s(8, "8,0 s"),
		Fade11_3s(9, "11,3 s"),
		Fade16s(10, "16,0 s"),
		Fade22_6s(11, "22,6 s"),
		Fade32s(12, "32,0 s"),
		Fade45_3s(13, "45,3 s"),
		Fade64s(14, "64,0 s"),
		Fade90_5s(15, "90,5 s");

		DaliFadeTime(final int element, final String description) { init(element, description); }
	}

	public enum BlinkingMode implements EnumBase<BlinkingMode> {
		Disabled(0, "Blinking Disabled"),
		WithoutAcknowledge(1, "Without Acknowledge"),
		WithAcknowledge(2, "Blinking With Acknowledge ");

		BlinkingMode(final int element, final String description) { init(element, description); }
	}

	public enum LightControlMode implements EnumBase<LightControlMode> {
		Automatic(0, "automatic light control"),
		Manual(1, "manual light control");

		LightControlMode(final int element, final String description) { init(element, description); }
	}

	public enum SwitchPushbuttonModel implements EnumBase<SwitchPushbuttonModel> {
		OnePushbutton(1, "one PB/binary input mode"),
		TwoPushbuttons(2, "two PBs/binary inputs mode");

		SwitchPushbuttonModel(final int element, final String description) { init(element, description); }
	}

	public enum SwitchPushbuttonAction implements EnumBase<SwitchPushbuttonAction> {
		Inactive(0, "inactive (no message sent)"),
		SwitchOffSent(1, "Switch-Off message sent"),
		SwitchOnSent(2, "Switch-On message sent"),
		InfoOnOff(3, "inverse value of Info On/Off is sent");

		SwitchPushbuttonAction(final int element, final String description) { init(element, description); }
	}

	/** Light Dimming Sensor Basic Mode. */
	public enum LdsbMode implements EnumBase<LdsbMode> {
		OnePushButton(1, "one push-button/binary input, Switch On/Off inverts on each transmission"),
		OnePushButtonDimUp(2, "one push-button/binary input, On/Dim-Up message sent"),
		OnePushButtonDimDown(3, "one push-button/binary input, Off/Dim-Down message sent"),
		TwoPushButtons(4, "two push-buttons/binary inputs mode");

		LdsbMode(final int element, final String description) { init(element, description); }
	}

	public enum SwitchOnMode implements EnumBase<SwitchOnMode> {
		LastActualValue(0, "last actual value"),
		AdditionalParameter(1, "value according additional parameter"),
		LastReceivedSetvalue(2, "last received absolute setvalue");

		SwitchOnMode(final int element, final String description) { init(element, description); }
	}

	public enum LoadTypeSet implements EnumBase<LoadTypeSet> {
		Automatic(0, "automatic"),
		LeadingEdge(1, "leading edge (inductive load)"),
		TrailingEdge(2, "trailing edge (capacitive load)");

		LoadTypeSet(final int element, final String description) { init(element, description); }
	}

	public enum LoadTypeDetected implements EnumBase<LoadTypeDetected> {
		Undefined(0, "undefined"),
		LeadingEdge(1, "leading edge (inductive load)"),
		TrailingEdge(2, "trailing edge (capacitive load)"),
		DetectionNotPossible(3, "detection not possible or error");

		LoadTypeDetected(final int element, final String description) { init(element, description); }
	}

	/** Sunblind Actuator Basic Exception Behavior. */
	public enum SabExceptBehavior implements EnumBase<SabExceptBehavior> {
		Up(0, "up"),
		Down(1, "down"),
		NoChange(2, "no change"),
		AdditionalParameter(3, "value according additional parameter"),
		Stop(4, "stop");

		SabExceptBehavior(final int element, final String description) { init(element, description); }
	}

	/** Sunblind Actuator Basic Behavior. */
	public enum SabBehaviorLockUnlock implements EnumBase<SabBehaviorLockUnlock> {
		Up(0, "up"),
		Down(1, "down"),
		NoChange(2, "no change"),
		AdditionalParameter(3, "value according additional parameter"),
		Stop(4, "stop"),
		UpdatedValue(5, "updated value"),
		ValueBeforeLocking(6, "value before locking");

		SabBehaviorLockUnlock(final int element, final String description) { init(element, description); }
	}

	/** Sunblind Sensor Basic Mode. */
	public enum SssbMode implements EnumBase<SssbMode> {
		OnePushButton(1, "one push button/binary input: Move-Up/Down inverts on each transmission"),
		OnePushButtonUp(2, "one push button/binary input: Move-Up/Step-Up message sent"),
		OnePushButtonDown(3, "one push button/binary input: Move-Down/Step-Down message sent"),
		TwoPushButtons(4, "two push buttons/binary inputs mode");

		SssbMode(final int element, final String description) { init(element, description); }
	}

	public enum BlindsControlMode implements EnumBase<BlindsControlMode> {
		AutomaticControl(0, "Automatic Control"),
		ManualControl(1, "Manual Control");

		BlindsControlMode(final int element, final String description) { init(element); }
	}

	// 20.1000
	public enum CommMode implements EnumBase<CommMode> {
		DataLinkLayer(0, "Data link layer"),
		Busmonitor(1, "Data link layer busmonitor"),
		RawFrames(2, "Data link layer raw frames"),
		cEmiTransportLayer(6, "cEMI transport layer"),
		NoLayer(0xff, "no layer");

		CommMode(final int element, final String description) { init(element, description); }
	}

	// 20.1001
	public enum AddInfoType implements EnumBase<AddInfoType> {
		Reserved(0, "Reserved"),
		PL(1, "PL medium DoA"),
		RF(2, "RF control and S/N or DoA"),
		Busmonitor(3, "Busmonitor error flags"),
		RelativeTimestamp(4, "Relative timestamp"),
		TimeDelay(5, "Time delay"),
		ExtRelativeTimestamp(6, "Extended relative timestamp"),
		BiBat(7, "BiBat information");

		AddInfoType(final int element, final String description) { init(element, description); }
	}

	// 20.1002
	public enum RFModeSelect implements EnumBase<RFModeSelect> {
		Asynchronous(0, "Asynchronous"),
		BiBatMaster(1, "Asynchronous + BiBat master"),
		BiBatSlave(2, "Asynchronous + BiBat slave");

		RFModeSelect(final int element, final String description) { init(element, description); }
	}

	// 20.1003
	public enum RFFilterSelect implements EnumBase<RFFilterSelect> {
		None(0, "no filter"),
		DomainAddress(1, "filtering by DoA"),
		SerialNumber(2, "filtering by KNX S/N table"),
		DoAAndSN(3, "filtering by DoA and S/N table");

		RFFilterSelect(final int element, final String description) { init(element, description); }
	}

	// 20.1004
	public enum Medium implements EnumBase<Medium> {
		TP1(0, "KNX TP1"),
		PL110(1, "KNX PL110"),
		RF(2, "KNX RF"),
		IP(5, "KNX IP");

		Medium(final int element, final String description) { init(element, description); }
	}

	// 20.1005
	public enum PushbuttonFunction implements EnumBase<PushbuttonFunction> {
		DefaultAction(1),
		On(2),
		Off(3),
		Toggle(4),
		DimmingUpDown(5),
		DimmingUp(6),
		DimmingDown(7),
		OnOff(8),
		TimedOnOff(9),
		ForcedOn(10),
		ForcedOff(11),
		ShutterUp(12),
		ShutterDown(13),
		ShutterUpDown(14),
		ForcedUp(16),
		ForcedDown(17),
		WindAlarm(18),
		RainAlarm(19),
		HvacModeComfortEconomy(20),
		HvacModeComfort(21),
		HvacModeEconomy(22),
		HvacModeBuildingProtectionAuto(23),
		ShutterStop(24),
		TimedComfortStandby(25),
		ForcedComfort(26),
		ForcedBuildingProtection(27),
		Scene1(28),
		Scene2(29),
		Scene3(30),
		Scene4(31),
		Scene5(32),
		Scene6(33),
		Scene7(34),
		Scene8(35),
		AbsoluteDimming25(36),
		AbsoluteDimming50(37),
		AbsoluteDimming75(38),
		AbsoluteDimming100(39),
		ShutterUpSwitch(40),
		ShutterDownSwitch(41),
		ShutterUpDownSwitch(42),
		ShutterDownUpSwitch(43),
		LightSensor(44),
		SystemClock(45),
		BatteryStatus(46),
		HvacModeStandby(47),
		HvacModeAuto(48),
		HvacModeComfortStandby(49),
		HvacModeBuildingProtection(50),
		TimedToggle(51),
		DimmingAbsoluteSwitch(52),
		SceneSwitch(53),
		SmokeAlarm(54),
		SubDetector(55);

		PushbuttonFunction(final int element) { init(element); }
	}

	// 20.1200
	public enum MBusBreakerValveState implements EnumBase<MBusBreakerValveState> {
		Closed(0, "Breaker/Valve is closed"),
		Open(1, "Breaker/Valve is open"),
		Released(2, "Break/Valve is released"),
		Invalid(255, "invalid");

		MBusBreakerValveState(final int element, final String description) { init(element, description); }
	}

	// 20.1202
	public enum GasMeasurementCondition implements EnumBase<GasMeasurementCondition> {
		Unknown(0, "unknown"),
		TemperatureConverted(1, "temperature converted"),
		BaseCondition(2, "at base condition"),
		MeasurementCondition(3, "at measurement condition");

		GasMeasurementCondition(final int element, final String description) { init(element, description); }
	}

	//
	// End of enumerations
	//

	public static class EnumDpt<T extends Enum<T> & EnumBase<T>> extends EnumDptBase<T> {
		public EnumDpt(final String typeID, final Class<T> elements, final String lower, final String upper) {
			super(typeID, elements, lower, upper);
		}

		public EnumDpt(final String typeID, final String description, final Class<T> elements, final String lower,
			final String upper) {
			super(typeID, description, elements, lower, upper);
		}
	}

	public static final EnumDpt<SystemClockMode> DptSystemClockMode = new EnumDpt<>("20.001",
			SystemClockMode.class, "0", "2");
	public static final EnumDpt<BuildingMode> DptBuildingMode = new EnumDpt<>("20.002",
			BuildingMode.class, "0", "2");
	public static final EnumDpt<OccupancyMode> DptOccupancyMode = new EnumDpt<>("20.003",
			OccupancyMode.class, "0", "2");
	public static final EnumDpt<Priority> DptPriority = new EnumDpt<>("20.004", "Priority",
			Priority.class, "0", "3");
	public static final EnumDpt<LightApplicationMode> DptLightApplicationMode = new EnumDpt<>(
			"20.005", LightApplicationMode.class, "0", "2");
	public static final EnumDpt<ApplicationArea> DptApplicationArea = new EnumDpt<>("20.006",
			ApplicationArea.class, "0", "14");
	public static final EnumDpt<AlarmClassType> DptAlarmClassType = new EnumDpt<>("20.007",
			AlarmClassType.class, "0", "3");
	public static final EnumDpt<PsuMode> DptPsuMode = new EnumDpt<>("20.008", "PSU Mode",
			PsuMode.class, "0", "2");
	public static final EnumDpt<ErrorClassSystem> DptErrorClassSystem = new EnumDpt<>("20.011",
			ErrorClassSystem.class, "0", "18");
	public static final EnumDpt<ErrorClassHvac> DptErrorClassHvac = new EnumDpt<>("20.012",
			"Error Class HVAC", ErrorClassHvac.class, "0", "4");
	public static final EnumDpt<TimeDelay> DptTimeDelay = new EnumDpt<>("20.013", TimeDelay.class, "0", "25");
	public static final EnumDpt<BeaufortWindForceScale> DptBeaufortWindForceScale = new EnumDpt<>(
			"20.014", BeaufortWindForceScale.class, "0", "12");
	public static final EnumDpt<SensorSelect> DptSensorSelect = new EnumDpt<>("20.017",
			SensorSelect.class, "0", "4");
	public static final EnumDpt<ActuatorConnectType> DptActuatorConnectType = new EnumDpt<>(
			"20.020", ActuatorConnectType.class, "1", "2");
	public static final EnumDpt<CloudCover> DptCloudCover = new EnumDpt<>("20.021", CloudCover.class,
			"0", "9");
	public static final EnumDpt<FuelType> DptFuelType = new EnumDpt<>("20.100", FuelType.class, "0", "3");
	public static final EnumDpt<BurnerType> DptBurnerType = new EnumDpt<>("20.101", BurnerType.class, "0", "3");
	public static final EnumDpt<HvacMode> DptHvacMode = new EnumDpt<>("20.102", "HVAC Mode",
			HvacMode.class, "0", "4");
	public static final EnumDpt<DomesticHotWaterMode> DptDomesticHotWaterMode = new EnumDpt<>("20.103", "DHW Mode",
			DomesticHotWaterMode.class, "0", "4");
	public static final EnumDpt<LoadPriority> DptLoadPriority = new EnumDpt<>("20.104",
			LoadPriority.class, "0", "2");
	public static final EnumDpt<HvacControlMode> DptHvacControlMode = new EnumDpt<>("20.105",
			"HVAC Control Mode", HvacControlMode.class, "0", "20");
	public static final EnumDpt<HvacEmergencyMode> DptHvacEmergencyMode = new EnumDpt<>("20.106",
			"HVAC Emergency Mode", HvacEmergencyMode.class, "0", "5");
	public static final EnumDpt<ChangeoverMode> DptChangeoverMode = new EnumDpt<>("20.107",
			ChangeoverMode.class, "0", "2");
	public static final EnumDpt<ValveMode> DptValveMode = new EnumDpt<>("20.108", ValveMode.class, "1", "5");
	public static final EnumDpt<DamperMode> DptDamperMode = new EnumDpt<>("20.109", DamperMode.class, "1", "4");
	public static final EnumDpt<HeaterMode> DptHeaterMode = new EnumDpt<>("20.110", HeaterMode.class, "1", "3");
	public static final EnumDpt<FanMode> DptFanMode = new EnumDpt<>("20.111", FanMode.class, "0", "2");
	public static final EnumDpt<MasterSlaveMode> DptMasterSlaveMode = new EnumDpt<>("20.112",
			"Master/Slave Mode", MasterSlaveMode.class, "0", "2");
	public static final EnumDpt<StatusRoomSetpoint> DptStatusRoomSetpoint = new EnumDpt<>("20.113",
			StatusRoomSetpoint.class, "0", "2");
	public static final EnumDpt<MeteringDeviceType> DptMeteringDeviceType = new EnumDpt<>("20.114",
			MeteringDeviceType.class, "0", "41/255");
	public static final EnumDpt<AirDamperActuatorType> DptAirDamperActuatorType = new EnumDpt<>("20.120",
			AirDamperActuatorType.class, "1", "2");
	public static final EnumDpt<BackupMode> DptBackupMode = new EnumDpt<>("20.121", BackupMode.class, "0", "1");
	public static final EnumDpt<StartSynchronization> DptStartSynchronization = new EnumDpt<>(
			"20.122", StartSynchronization.class, "0", "2");
	public static final EnumDpt<BehaviorLockUnlock> DptBehaviorLockUnlock = new EnumDpt<>(
			"20.600", "Behavior Lock/Unlock", BehaviorLockUnlock.class, "0", "6");
	public static final EnumDpt<BehaviorBusPowerUpDown> DptBehaviorBusPowerUpDown = new EnumDpt<>(
			"20.601", "Behavior Bus Power Up/Down", BehaviorBusPowerUpDown.class, "0", "4");
	/** DPT Digital Addressable Lighting Interface Fade Time. */
	public static final EnumDpt<DaliFadeTime> DptDaliFadeTime = new EnumDpt<>("20.602",
			"DALI Fade Time", DaliFadeTime.class, "0", "15");
	public static final EnumDpt<BlinkingMode> DptBlinkingMode = new EnumDpt<>("20.603",
			BlinkingMode.class, "0", "2");
	public static final EnumDpt<LightControlMode> DptLightControlMode = new EnumDpt<>("20.604",
			LightControlMode.class, "0", "1");
	public static final EnumDpt<SwitchPushbuttonModel> DptSwitchPushbuttonModel = new EnumDpt<>("20.605",
			"Switch PB Model", SwitchPushbuttonModel.class, "1", "2");
	public static final EnumDpt<SwitchPushbuttonAction> DptSwitchPushbuttonAction = new EnumDpt<>("20.606",
			"PB Action", SwitchPushbuttonAction.class, "0", "3");
	public static final EnumDpt<LdsbMode> DptDimmPushbuttonModel = new EnumDpt<>("20.607", "Dimm PB Model",
			LdsbMode.class, "1", "4");
	public static final EnumDpt<SwitchOnMode> DptSwitchOnMode = new EnumDpt<>("20.608",
			SwitchOnMode.class, "0", "2");
	public static final EnumDpt<LoadTypeSet> DptLoadTypeSet = new EnumDpt<>("20.609",
			LoadTypeSet.class, "0", "2");
	public static final EnumDpt<LoadTypeDetected> DptLoadTypeDetected = new EnumDpt<>("20.610",
			LoadTypeDetected.class, "0", "3");
	/** DPT Sunblind Actuator Basic Exception Behavior. */
	public static final EnumDpt<SabExceptBehavior> DptSabExceptBehavior = new EnumDpt<>("20.801",
			"SAB Except Behavior", SabExceptBehavior.class, "0", "4");
	/** DPT Sunblind Actuator Basic Behavior. */
	public static final EnumDpt<SabBehaviorLockUnlock> DptSabBehaviorLockUnlock = new EnumDpt<>(
			"20.802", "SAB Behavior Lock/Unlock", SabBehaviorLockUnlock.class, "0", "6");
	/** DPT Sunblind Sensor Basic Mode. */
	public static final EnumDpt<SssbMode> DptSssbMode = new EnumDpt<>("20.803", "SSSB Mode",
			SssbMode.class, "1", "4");
	public static final EnumDpt<BlindsControlMode> DptBlindsControlMode = new EnumDpt<>("20.804",
			BlindsControlMode.class, "0", "1");

	// System domain

	public static final EnumDpt<CommMode> DptCommMode = new EnumDpt<>("20.1000", CommMode.class, "0", "255");
	public static final EnumDpt<AddInfoType> DptAddInfoType = new EnumDpt<>("20.1001",
			"Additional Info Type", AddInfoType.class, "0", "7");
	public static final EnumDpt<RFModeSelect> DptRFModeSelect = new EnumDpt<>("20.1002",
			"RF Mode Select", RFModeSelect.class, "0", "2");
	public static final EnumDpt<RFFilterSelect> DptRFFilterSelect = new EnumDpt<>("20.1003",
			"RF Filter Select", RFFilterSelect.class, "0", "3");


	public static final EnumDpt<Medium> DptMedium = new EnumDpt<>("20.1004",
			"KNX medium", Medium.class, "0", "5");

	/** Configuration of action of push button in PB mode. */
	public static final EnumDpt<PushbuttonFunction> DptPushbuttonFunction = new EnumDpt<>("20.1005", "PB function",
			PushbuttonFunction.class, "1", "55");

	// Metering domain

	public static final EnumDpt<MBusBreakerValveState> DptMBusBreakerValveState = new EnumDpt<>(
			"20.1200", "M-Bus Breaker/Valve State", MBusBreakerValveState.class, "0", "255");
	public static final EnumDpt<GasMeasurementCondition> DptGasMeasurementCondition = new EnumDpt<>(
			"20.1202", GasMeasurementCondition.class, "0", "3");

	// LTE-HEE Mode

	// not standardized, sub number 60104 is mfr specific
	static final EnumDpt<HvacCommand> DptHvacCommand = new EnumDpt<>(
			"20.60104", HvacCommand.class, "0", "6");

	// not standardized, sub number 60105 is mfr specific
	static final EnumDpt<EnableHeatCoolStage> DptEnableHeatCoolStage = new EnumDpt<>(
			"20.60105", EnableHeatCoolStage.class, "0", "3");



	private static final Map<String, DPT> types = loadDatapointTypes(DPTXlator8BitEnum.class);
	static {
		// we have to add it manually because loadDatapointTypes only loads public DPTs
		types.put(DptHvacCommand.getID(), DptHvacCommand);
		types.put(DptEnableHeatCoolStage.getID(), DptEnableHeatCoolStage);
	}


	/**
	 * Creates a translator for the given datapoint type.
	 *
	 * @param dpt the requested datapoint type
	 * @throws KNXFormatException on not supported or not available DPT
	 */
	public DPTXlator8BitEnum(final DPT dpt) throws KNXFormatException
	{
		this(dpt.getID());
	}

	/**
	 * Creates a translator for the given datapoint type ID.
	 *
	 * @param dptID available implemented datapoint type ID
	 * @throws KNXFormatException on wrong formatted or not expected (available) <code>dptID</code>
	 */
	public DPTXlator8BitEnum(final String dptID) throws KNXFormatException
	{
		super(1);
		setTypeID(types, dptID);
		final var values = ((EnumDpt<?>) dpt).values();
		data = new short[] { (short) values[0].value() };
	}

	@Override
	public String getValue()
	{
		return makeString(0);
	}

	@Override
	public void setValue(final double element) throws KNXFormatException {
		setValue((int) element);
	}

	/**
	 * Sets one new translation item from an unsigned value, replacing any old items.
	 *
	 * @param element the element to set as unsigned value, the dimension is determined by the set
	 *        DPT, 0 &le; element &le; defined maximum of DPT
	 * @throws KNXFormatException if element is not in the enumeration specified by the used DPT ID
	 * @see #getType()
	 */
	public final void setValue(final int element) throws KNXFormatException
	{
		data = new short[] { toDPT(element) };
	}

	/**
	 * Sets one new translation item from an enumeration element, replacing any old items.
	 *
	 * @param element the element to set, the enumeration has to match the set DPT
	 * @throws KNXFormatException if element is not in the enumeration specified by the used DPT ID
	 * @see #getType()
	 */
	public final void setValue(final EnumBase<? extends EnumBase<?>> element)
		throws KNXFormatException
	{
		data = new short[] { toDPT(element.value()) };
	}

	/**
	 * Returns the first translation item.
	 *
	 * @return element as unsigned 8 Bit
	 * @throws KNXFormatException on formatting datapoint value
	 * @see #getType()
	 */
	public final int getValueUnsigned() throws KNXFormatException
	{
		return fromDPT(0);
	}

	@Override
	public final double getNumericValue() throws KNXFormatException
	{
		return getValueUnsigned();
	}

	@Override
	public String[] getAllValues()
	{
		final String[] s = new String[data.length];
		for (int i = 0; i < data.length; ++i)
			s[i] = makeString(i);
		return s;
	}

	@Override
	public final Map<String, DPT> getSubTypes()
	{
		return types;
	}

	/**
	 * @return the subtypes of the 8 Bit enumeration translator type
	 * @see DPTXlator#getSubTypesStatic()
	 */
	protected static Map<String, DPT> getSubTypesStatic()
	{
		return types;
	}

	private String makeString(final int index)
	{
		return ((EnumDpt<?>) dpt).textOf(data[index]);
	}

	@Override
	protected void toDPT(final String value, final short[] dst, final int index)
		throws KNXFormatException
	{
		try {
			dst[index] = toDPT(Short.decode(removeUnit(value)).shortValue());
		}
		catch (final NumberFormatException nfe) {
			// try name of enum constant or element description
			final EnumBase<?> e = ((EnumDpt<?>) dpt).find(value);
			if (e == null)
				throw newException("value is no element of "
						+ ((EnumDpt<?>) dpt).name() + " enumeration", value);
			dst[index] = (short) e.value();
		}
	}

	private short toDPT(final int value) throws KNXFormatException
	{
		validate(value);
		return (short) value;
	}

	// this method checks whether the data item actually is part of the DPT enumeration
	private short fromDPT(final int index) throws KNXFormatException
	{
		final short v = data[index];
		validate(v);
		return v;
	}

	private void validate(final int value) throws KNXFormatException
	{
		final EnumDpt<?> enumDpt = (EnumDpt<?>) dpt;
		if (!enumDpt.contains(value))
			throw newException("value is no element of " + enumDpt.name()
					+ " enumeration", Integer.toString(value));
	}
}
