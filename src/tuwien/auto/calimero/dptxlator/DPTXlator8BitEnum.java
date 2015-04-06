/*
    Calimero 2 - A library for KNX network access
    Copyright (c) 2015 B. Malinowsky

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

import java.lang.reflect.Field;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

import tuwien.auto.calimero.exception.KNXFormatException;
import tuwien.auto.calimero.exception.KNXIllegalArgumentException;
import tuwien.auto.calimero.log.LogService.LogLevel;

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
public class DPTXlator8BitEnum extends DPTXlator
{
	// the translator does not include the system domain subtypes (they are not used anyway?)

	// TODO review and finish subtype enumerations, make enums public

	// use a common base to store and access enumeration elements independent of specific type
	private interface EnumBase<E extends EnumBase<E>>
	{
		final Map<EnumBase<?>, Integer> values = new HashMap<>();
		final Map<EnumBase<?>, String> descriptions = new HashMap<>();

		default void init(final int element, final String description)
		{
			values.put(this, element);
			descriptions.put(this, description);
		}

		default int value()
		{
			return values.get(this);
		}

		default String description()
		{
			return descriptions.get(this);
		}
	}

	//
	// The specified enumerations for the DPTs of this translator
	//

	static enum SCLOMode implements EnumBase<SCLOMode> {
		Autonomous(0, "autonomous"),
		Slave(1, "slave"),
		Master(2, "master");

		private SCLOMode(final int element, final String description)
		{ //RP2
			init(element, description);
		} //RP1
	}

	static enum BuildingMode implements EnumBase<BuildingMode> {
		BuildingInUse(0, "Building in use"),
		BuildingNotUsed(1, "Building not used"),
		BuildingProtection(2, "Building protection");

		private BuildingMode(final int element, final String description)
		{ //RP2
			init(element, description);
		} //RP1
	}

	static enum OccupancyMode implements EnumBase<OccupancyMode> {
		Occupied(0, "occupied"),
		Standby(1, "standby"),
		NotOccupied(2, "not occupied");

		private OccupancyMode(final int element, final String description)
		{ //RP2
			init(element, description);
		}
		//RP1
	}

	static enum Priority implements EnumBase<Priority> {
		High(0, "High"),
		Medium(1, "Medium"),
		Low(2, "Low"),
		Void(3, "void");

		private Priority(final int element, final String description)
		{ //RP2
			init(element, description);
		}
		//RP1
	}

	static enum LightApplicationMode implements EnumBase<LightApplicationMode> {
		Normal(0, "normal"),
		PresenceSimulation(1, "presence simulation"),
		NightRound(2, "night round");

		private LightApplicationMode(final int element, final String description)
		{ //RP2
			init(element, description);
		}
		//RP1
	}

	static enum ApplicationArea implements EnumBase<ApplicationArea> {
		NoFault(0, "no fault"),
		CommonInterest(1, "system and functions of common interest"),
		HVACGeneralFBs(10, "HVAC general FBs"),
		HVACHotWaterHeating(11, "HVAC Hot Water Heating"),
		HVACDirectElectricalHeating(12, "HVAC Direct Electrical Heating"),
		HVACTerminalUnits(13, "HVAC Terminal Units"),
		HVACVAC(14, "HVAC VAC"),
		Lighting(20, "Lighting"),
		Security(30, "Security"),
		LoadManagement(40, "Load Management"),
		ShuttersAndBlinds(50, "Shutters and blinds");

		private ApplicationArea(final int element, final String description)
		{ //RP2
			init(element, description);
		}
		//RP1
	}

	static enum AlarmClassType implements EnumBase<AlarmClassType> {
		SimpleAlarm(1, "simple alarm"),
		BasicAlarm(2, "basic alarm"),
		ExtendedAlarm(3, "extended alarm");

		private AlarmClassType(final int element, final String description)
		{ //RP2
			init(element, description);
		} //RP1
	}

	static enum PSUMode implements EnumBase<PSUMode> {
		Disabled(0, "disabled (PSU/DPSU fixed off)"),
		Enabled(1, "enabled (PSU/DPSU fixed on)"),
		Auto(2, "auto (PSU/DPSU automatic on/off)");

		private PSUMode(final int element, final String description)
		{ //RP2
			init(element, description);
		} //RP1
	}

	static enum ErrorClassSystem implements EnumBase<ErrorClassSystem> {
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

		private ErrorClassSystem(final int element, final String description)
		{ //RP2
			init(element, description);
		} //RP1
	}

	static enum ErrorClassHVAC implements EnumBase<ErrorClassHVAC> {
		NoFault(0, "no fault"),
		SensorFault(1, "sensor fault"),
		ProcessFault(2, "process fault / controller fault"),
		ActuatorFault(3, "actuator fault"),
		OtherFault(4, "other fault");

		private ErrorClassHVAC(final int element, final String description)
		{ //RP2
			init(element, description);
		} //RP1
	}

	static enum TimeDelay implements EnumBase<TimeDelay> {
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

		private TimeDelay(final int element, final String description)
		{ //RP2
			init(element, description);
		} //RP1
	}

	static enum BeaufortWindForceScale implements EnumBase<BeaufortWindForceScale> {
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

		private BeaufortWindForceScale(final int element, final String description)
		{ //RP2
			init(element, description);
		} //RP1
	}

	static enum SensorSelect implements EnumBase<SensorSelect> {
		Inactive(0, "inactive"),
		DigitalInput(1, "digital input (not inverted)"),
		DigitalInputInverted(2, "digital input inverted"),
		AnalogInput(3, "analog input, 0 % to 100 %"),
		TemperatureSensorInput(4, "temperature sensor input");

		private SensorSelect(final int element, final String description)
		{ //RP2
			init(element, description);
		} //RP1
	}

	static enum ActuatorConnectType implements EnumBase<ActuatorConnectType> {
		SensorConnection(1, "Sensor Connection"),
		ControllerConnection(2, "Controller Connection");

		private ActuatorConnectType(final int element, final String description)
		{ //RP2
			init(element, description);
		} //RP1
	}

	static enum FuelType implements EnumBase<FuelType> {
		Auto(0, "auto"),
		Oil(1, "oil"),
		Gas(2, "gas"),
		SolidStateFuel(3, "solid state fuel");

		private FuelType(final int element, final String description)
		{ //RP2
			init(element, description);
		} //RP1
	}

	static enum BurnerType implements EnumBase<BurnerType> {
		OneStage(1, "1 stage"),
		TwoStage(2, "2 stage"),
		Modulating(3, "modulating");

		private BurnerType(final int element, final String description)
		{ //RP2
			init(element, description);
		} //RP1
	}

	static enum HVACMode implements EnumBase<HVACMode> {
		Auto(0, "Auto"),
		Comfort(1, "Comfort"),
		Standby(2, "Standby"),
		Economy(3, "Economy"),
		BuildingProtection(4, "Building Protection");

		private HVACMode(final int element, final String description)
		{ //RP2
			init(element, description);
		} //RP1
	}

	static enum DHWMode implements EnumBase<DHWMode> {
		Auto(0, "Auto"),
		LegioProtect(1, "Legio Protect"),
		Normal(2, "Normal"),
		Reduced(3, "Reduced"),
		OffOrFrostProtect(4, "Off / Frost Protect");

		private DHWMode(final int element, final String description)
		{ //RP2
			init(element, description);
		} //RP1
	}

	static enum LoadPriority implements EnumBase<LoadPriority> {
		None(0, "None"),
		ShiftLoadPriority(1, "Shift load priority"),
		AbsoluteLoadPriority(2, "Absolute load priority");

		private LoadPriority(final int element, final String description)
		{ //RP2
			init(element, description);
		} //RP1
	}

	static enum HVACControlMode implements EnumBase<HVACControlMode> {
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

		private HVACControlMode(final int element, final String description)
		{ //RP2
			init(element, description);
		} //RP1
	}

	static enum HVACEmergencyMode implements EnumBase<HVACEmergencyMode> {
		Normal(0, "Normal"),
		Pressure(1, "Emergency Pressure"),
		Depressure(2, "Emergency Depressure"),
		Purge(3, "Emergency Purge"),
		Shutdown(4, "Emergency Shutdown"),
		Fire(5, "Emergency Fire");

		private HVACEmergencyMode(final int element, final String description)
		{ //RP2
			init(element, description);
		} //RP1
	}

	static enum ChangeoverMode implements EnumBase<ChangeoverMode> {
		Auto(0, "Auto"),
		CoolingOnly(1, "Cooling Only"),
		HeatingOnly(2, "Heating Only");

		private ChangeoverMode(final int element, final String description)
		{ //RP2
			init(element, description);
		} //RP1
	}

	static enum ValveMode implements EnumBase<ValveMode> {
		HeatStageA(1, "Heat stage A for normal heating"),
		HeatStageB(2, "Heat stage B for heating with two stages (A + B)"),
		CoolStageA(3, "Cool stage A for normal cooling"),
		CoolStageB(4, "Cool stage B for cooling with two stages (A + B)"),
		HeatCool(5, "Heat/Cool for changeover applications");

		private ValveMode(final int element, final String description)
		{ //RP2
			init(element, description);
		} //RP1
	}

	static enum DamperMode implements EnumBase<DamperMode> {
		FreshAir(1, "Fresh air, e.g., fancoils"),
		SupplyAir(2, "Supply Air, e.g. Variable Air Volume (VAV)"),
		ExtractAir(3, "Extract Air, e.g. Variable Air Volume (VAV)"),
		ExtractAir2(4, "Extract Air, e.g. Variable Air Volume (VAV)");

		private DamperMode(final int element, final String description)
		{ //RP2
			init(element, description);
		} //RP1
	}

	static enum HeaterMode implements EnumBase<HeaterMode> {
		HeatStageA(1, "Heat Stage A On/Off"),
		HeatStageAProportional(2, "Heat Stage A Proportional"),
		HeatStageBProportional(3, "Heat Stage B Proportional");

		private HeaterMode(final int element, final String description)
		{ //RP2
			init(element, description);
		} //RP1
	}

	static enum FanMode implements EnumBase<FanMode> {
		NotRunning(0, "not running"),
		PermanentlyRunning(1, "permanently running"),
		Intervals(2, "running in intervals");

		private FanMode(final int element, final String description)
		{ //RP2
			init(element, description);
		} //RP1
	}

	static enum MasterSlaveMode implements EnumBase<MasterSlaveMode> {
		Autonomous(0, "autonomous"),
		Master(1, "master"),
		Slave(2, "slave");

		private MasterSlaveMode(final int element, final String description)
		{ //RP2
			init(element, description);
		} //RP1
	}

	static enum StatusRoomSetpoint implements EnumBase<StatusRoomSetpoint> {
		Normal(0, "normal setpoint"),
		Alternative(1, "alternative setpoint"),
		BuildingProtection(2, "building protection setpoint");

		private StatusRoomSetpoint(final int element, final String description)
		{ //RP2
			init(element, description);
		} //RP1
	}

	static enum MeteringDeviceType implements EnumBase<MeteringDeviceType> {
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

		private MeteringDeviceType(final int element, final String description)
		{ //RP2
			init(element, description);
		} //RP1
	}

	static enum ADAType implements EnumBase<ADAType> {

		AirDamper(1, "Air Damper"),
		VAV(2, "VAV");

		private ADAType(final int element, final String description)
		{ //RP2
			init(element, description);
		} //RP1
	}

	static enum BackupMode implements EnumBase<BackupMode> {
		BackupValue(0, "Backup Value"),
		KeepLastState(1, "Keep Last State");

		private BackupMode(final int element, final String description)
		{ //RP2
			init(element, description);
		} //RP1
	}

	static enum StartSynchronization implements EnumBase<StartSynchronization> {
		PositionUnchanged(0, "Position unchanged"),
		SingleClose(1, "Single close"),
		SingleOpen(2, "Single open");

		private StartSynchronization(final int element, final String description)
		{ //RP2
			init(element, description);
		} //RP1
	}

	static enum BehaviourLockUnlock implements EnumBase<BehaviourLockUnlock> {
		Off(0, "off"),
		On(1, "on"),
		NoChange(2, "no change"),
		AdditionalParameter(3, "value according additional parameter"),
		MemoryFunction(4, "memory function value"),
		UpdatedValue(5, "updated value"),
		ValueBeforeLocking(6, "value before locking");

		private BehaviourLockUnlock(final int element, final String description)
		{ //RP2
			init(element, description);
		} //RP1
	}

	static enum BehaviourBusPowerUpDown implements EnumBase<BehaviourBusPowerUpDown> {
		Off(0, "off"),
		On(1, "on"),
		NoChange(2, "no change"),
		AdditionalParameter(3, "value according additional parameter"),
		Last(4, "last (value before bus power down)");

		private BehaviourBusPowerUpDown(final int element, final String description)
		{ //RP2
			init(element, description);
		} //RP1
	}

	static enum DaliFadeTime implements EnumBase<DaliFadeTime> {
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

		private DaliFadeTime(final int element, final String description)
		{ //RP2
			init(element, description);
		} //RP1
	}

	static enum BlinkingMode implements EnumBase<BlinkingMode> {
		Disabled(0, "Blinking Disabled"),
		WithoutAcknowledge(1, "Without Acknowledge"),
		WithAcknowledge(2, "Blinking With Acknowledge ");

		private BlinkingMode(final int element, final String description)
		{ //RP2
			init(element, description);
		} //RP1
	}

	static enum LightControlMode implements EnumBase<LightControlMode> {
		Automatic(0, "automatic light control"),
		Manual(1, "manual light control");

		private LightControlMode(final int element, final String description)
		{ //RP2
			init(element, description);
		} //RP1
	}

	static enum SwitchPBModel implements EnumBase<SwitchPBModel> {
		OnePB(1, "one PB/binary input mode"),
		TwoPBs(2, "two PBs/binary inputs mode");

		private SwitchPBModel(final int element, final String description)
		{ //RP2
			init(element, description);
		} //RP1
	}

	static enum SwitchPBAction implements EnumBase<SwitchPBAction> {
		Inactive(0, "inactive (no message sent)"),
		SwitchOffSent(1, "Switch-Off message sent"),
		SwitchOnSent(2, "Switch-On message sent"),
		InfoOnOff(3, "inverse value of Info On/Off  is sent");

		private SwitchPBAction(final int element, final String description)
		{ //RP2
			init(element, description);
		} //RP1
	}

	static enum LDSBMode implements EnumBase<LDSBMode> {
		OnePushButton(1, "one push-button/binary input, Switch On/Off inverts on each transmission"),
		OnePushButtonDimUp(2, "one push-button/binary input, On/Dim-Up message sent"),
		OnePushButtonDimDown(3, "one push-button/binary input, Off/Dim-Down message sent"),
		TwoPushButtons(4, "two push-buttons/binary inputs mode");

		private LDSBMode(final int element, final String description)
		{ //RP2
			init(element, description);
		} //RP1
	}

	static enum SwitchOnMode implements EnumBase<SwitchOnMode> {
		LastActualValue(0, "last actual value"),
		AdditionalParameter(1, "value according additional parameter"),
		LastReceivedSetvalue(2, "last received absolute setvalue");

		private SwitchOnMode(final int element, final String description)
		{ //RP2
			init(element, description);
		} //RP1
	}

	static enum LoadTypeSet implements EnumBase<LoadTypeSet> {
		Automatic(0, "automatic"),
		LeadingEdge(1, "leading edge (inductive load)"),
		TrailingEdge(2, "trailing edge (capacitive load)");

		private LoadTypeSet(final int element, final String description)
		{ //RP2
			init(element, description);
		} //RP1
	}

	static enum LoadTypeDetected implements EnumBase<LoadTypeDetected> {
		Undefined(0, "undefined"),
		LeadingEdge(1, "leading edge (inductive load)"),
		TrailingEdge(2, "trailing edge (capacitive load)"),
		DetectionNotPossible(3, "detection not possible or error");

		private LoadTypeDetected(final int element, final String description)
		{ //RP2
			init(element, description);
		} //RP1
	}

	static enum SABExceptBehaviour implements EnumBase<SABExceptBehaviour> {
		Up(0, "up"),
		Down(1, "down"),
		NoChange(2, "no change"),
		AdditionalParameter(3, "value according additional parameter"),
		Stop(4, "stop");

		private SABExceptBehaviour(final int element, final String description)
		{ //RP2
			init(element, description);
		} //RP1
	}

	static enum SABBehaviourLockUnlock implements EnumBase<SABBehaviourLockUnlock> {
		Up(0, "up"),
		Down(1, "down"),
		NoChange(2, "no change"),
		AdditionalParameter(3, "value according additional parameter"),
		Stop(4, "stop"),
		UpdatedValue(5, "updated value"),
		ValueBeforeLocking(6, "value before locking");

		private SABBehaviourLockUnlock(final int element, final String description)
		{ //RP2
			init(element, description);
		} //RP1
	}

	static enum SSSBMode implements EnumBase<SSSBMode> {
		OnePushButton(1, "one push button/binary input: Move-Up/Down inverts on each transmission"),
		OnePushButtonUp(2, "one push button/binary input: Move-Up/Step-Up message sent"),
		OnePushButtonDown(3, "one push button/binary input: Move-Down/Step-Down message sent"),
		TwoPushButtons(4, "two push buttons/binary inputs mode");

		private SSSBMode(final int element, final String description)
		{ //RP2
			init(element, description);
		} //RP1
	}

	static enum BlindsControlMode implements EnumBase<BlindsControlMode> {
		AutomaticControl(0, "Automatic Control"),
		ManualControl(1, "Manual Control");

		private BlindsControlMode(final int element, final String description)
		{ //RP2
			init(element, description);
		} //RP1
	}

	//
	// End of enumerations
	//

	public static class EnumDpt<T extends Enum<T> & EnumBase<T>> extends DPT
	{
		// using a class reference here, we only instantiate the enum when actually queried
		private final Class<T> elements;

		public EnumDpt(final String typeID, final String description, final Class<T> elements,
			final String lower, final String upper)
		{
			super(typeID, description, lower, upper);
			this.elements = elements;
		}

		private T find(final int element)
		{
			final EnumSet<T> set = EnumSet.allOf(elements);
			for (final T e : set) {
				if (e.value() == element)
					return e;
			}
			return null;
		}

		private T find(final String description)
		{
			final EnumSet<T> set = EnumSet.allOf(elements);
			for (final T e : set) {
				// the enum constant we can compare case sensitive
				if (e.name().equals(description))
					return e;
				if (e.description().equalsIgnoreCase(description))
					return e;
			}
			return null;
		}

		private boolean contains(final int element)
		{
			return find(element) != null;
		}

		private String textOf(final int element)
		{
			final T e = find(element);
			if (e != null)
				return e.description();
			throw new KNXIllegalArgumentException(getID() + " " + elements.getSimpleName()
					+ " has no element " + element + " specified");
		}

		@Override
		public String toString()
		{
			final StringBuffer sb = new StringBuffer(30);
			sb.append(getID()).append(": ").append(getDescription()).append(", enumeration [");
			sb.append(getLowerValue()).append("..").append(getUpperValue()).append("]");
			return sb.toString();
		}
	}

	public static final EnumDpt<SCLOMode> DPT_SCLOMode = new EnumDpt<>("20.001", "SCLO Mode",
			SCLOMode.class, "0", "2");
	public static final EnumDpt<BuildingMode> DPT_BuildingMode = new EnumDpt<>("20.002",
			"Building Mode", BuildingMode.class, "0", "2");
	public static final EnumDpt<OccupancyMode> DPT_OccupancyMode = new EnumDpt<>("20.003",
			"Occupancy Mode", OccupancyMode.class, "0", "2");
	public static final EnumDpt<Priority> DPT_Priority = new EnumDpt<>("20.004", "Priority",
			Priority.class, "0", "3");
	public static final EnumDpt<LightApplicationMode> DPT_LightApplicationMode = new EnumDpt<>(
			"20.005", "Light Application Mode", LightApplicationMode.class, "0", "2");
	public static final EnumDpt<ApplicationArea> DPT_ApplicationArea = new EnumDpt<>("20.006",
			"Application Area", ApplicationArea.class, "0", "14");
	public static final EnumDpt<AlarmClassType> DPT_AlarmClassType = new EnumDpt<>("20.007",
			"Alarm Class Type", AlarmClassType.class, "0", "3");
	public static final EnumDpt<PSUMode> DPT_PSUMode = new EnumDpt<>("20.008", "PSU Mode",
			PSUMode.class, "0", "2");
	public static final EnumDpt<ErrorClassSystem> DPT_ErrorClassSystem = new EnumDpt<>("20.011",
			"Error Class System", ErrorClassSystem.class, "0", "18");
	public static final EnumDpt<ErrorClassHVAC> DPT_ErrorClassHVAC = new EnumDpt<>("20.012",
			"Error Class HVAC", ErrorClassHVAC.class, "0", "4");
	public static final EnumDpt<TimeDelay> DPT_TimeDelay = new EnumDpt<>("20.013", "Time Delay",
			TimeDelay.class, "0", "25");
	public static final EnumDpt<BeaufortWindForceScale> DPT_BeaufortWindForceScale = new EnumDpt<>(
			"20.014", "Beaufort Wind Force Scale", BeaufortWindForceScale.class, "0", "12");
	public static final EnumDpt<SensorSelect> DPT_SensorSelect = new EnumDpt<>("20.017",
			"Sensor Select", SensorSelect.class, "0", "4");
	public static final EnumDpt<ActuatorConnectType> DPT_ActuatorConnectType = new EnumDpt<>(
			"20.020", "Actuator Connect Type", ActuatorConnectType.class, "1", "2");
	public static final EnumDpt<FuelType> DPT_FuelType = new EnumDpt<>("20.100", "Fuel Type",
			FuelType.class, "0", "3");
	public static final EnumDpt<BurnerType> DPT_BurnerType = new EnumDpt<>("20.101", "Burner Type",
			BurnerType.class, "0", "3");
	public static final EnumDpt<HVACMode> DPT_HVACMode = new EnumDpt<>("20.102", "HVAC Mode",
			HVACMode.class, "0", "4");
	public static final EnumDpt<DHWMode> DPT_DHWMode = new EnumDpt<>("20.103", "DHW Mode",
			DHWMode.class, "0", "4");
	public static final EnumDpt<LoadPriority> DPT_LoadPriority = new EnumDpt<>("20.104",
			"Load Priority", LoadPriority.class, "0", "2");
	public static final EnumDpt<HVACControlMode> DPT_HVACControlMode = new EnumDpt<>("20.105",
			"HVAC Control Mode", HVACControlMode.class, "0", "20");
	public static final EnumDpt<HVACEmergencyMode> DPT_HVACEmergencyMode = new EnumDpt<>("20.106",
			"HVAC Emergency Mode", HVACEmergencyMode.class, "0", "5");
	public static final EnumDpt<ChangeoverMode> DPT_ChangeoverMode = new EnumDpt<>("20.107",
			"Changeover Mode", ChangeoverMode.class, "0", "2");
	public static final EnumDpt<ValveMode> DPT_ValveMode = new EnumDpt<>("20.108", "Valve Mode",
			ValveMode.class, "1", "5");
	public static final EnumDpt<DamperMode> DPT_DamperMode = new EnumDpt<>("20.109", "Damper Mode",
			DamperMode.class, "1", "4");
	public static final EnumDpt<HeaterMode> DPT_HeaterMode = new EnumDpt<>("20.110", "Heater Mode",
			HeaterMode.class, "1", "3");
	public static final EnumDpt<FanMode> DPT_FanMode = new EnumDpt<>("20.111", "Fan Mode",
			FanMode.class, "0", "2");
	public static final EnumDpt<MasterSlaveMode> DPT_MasterSlaveMode = new EnumDpt<>("20.112",
			"Master/Slave Mode", MasterSlaveMode.class, "0", "2");
	public static final EnumDpt<StatusRoomSetpoint> DPT_StatusRoomSetpoint = new EnumDpt<>(
			"20.113", "Status Room Setpoint", StatusRoomSetpoint.class, "0", "2");
	public static final EnumDpt<MeteringDeviceType> DPT_MeteringDeviceType = new EnumDpt<>(
			"20.114", "Metering Device Type", MeteringDeviceType.class, "0", "41/255");
	public static final EnumDpt<ADAType> DPT_ADAType = new EnumDpt<>("20.120", "ADA Type",
			ADAType.class, "1", "2");
	public static final EnumDpt<BackupMode> DPT_BackupMode = new EnumDpt<>("20.121", "Backup Mode",
			BackupMode.class, "0", "1");
	public static final EnumDpt<StartSynchronization> DPT_StartSynchronization = new EnumDpt<>(
			"20.122", "Start Synchronization", StartSynchronization.class, "0", "2");
	public static final EnumDpt<BehaviourLockUnlock> DPT_BehaviourLockUnlock = new EnumDpt<>(
			"20.600", "Behaviour Lock/Unlock", BehaviourLockUnlock.class, "0", "6");
	public static final EnumDpt<BehaviourBusPowerUpDown> DPT_BehaviourBusPowerUpDown = new EnumDpt<>(
			"20.601", "Behaviour Bus Power Up/Down", BehaviourBusPowerUpDown.class, "0", "4");
	public static final EnumDpt<DaliFadeTime> DPT_DaliFadeTime = new EnumDpt<>("20.602",
			"DALI Fade Time", DaliFadeTime.class, "0", "15");
	public static final EnumDpt<BlinkingMode> DPT_BlinkingMode = new EnumDpt<>("20.603",
			"Blinking Mode", BlinkingMode.class, "0", "2");
	public static final EnumDpt<LightControlMode> DPT_LightControlMode = new EnumDpt<>("20.604",
			"Light Control Mode", LightControlMode.class, "0", "1");
	public static final EnumDpt<SwitchPBModel> DPT_SwitchPBModel = new EnumDpt<>("20.605",
			"Switch PB Model", SwitchPBModel.class, "1", "2");
	public static final EnumDpt<SwitchPBAction> DPT_SwitchPBAction = new EnumDpt<>("20.606",
			"PB Action", SwitchPBAction.class, "0", "3");
	public static final EnumDpt<LDSBMode> DPT_DimmPBModel = new EnumDpt<>("20.607",
			"Dimm PB Model", LDSBMode.class, "1", "4");
	public static final EnumDpt<SwitchOnMode> DPT_SwitchOnMode = new EnumDpt<>("20.608",
			"Switch On Mode", SwitchOnMode.class, "0", "2");
	public static final EnumDpt<LoadTypeSet> DPT_LoadTypeSet = new EnumDpt<>("20.609",
			"Load Type Set", LoadTypeSet.class, "0", "2");
	public static final EnumDpt<LoadTypeDetected> DPT_LoadTypeDetected = new EnumDpt<>("20.610",
			"Load Type Detected", LoadTypeDetected.class, "0", "3");
	public static final EnumDpt<SABExceptBehaviour> DPT_SABExceptBehaviour = new EnumDpt<>(
			"20.801", "SABExcept  Behaviour", SABExceptBehaviour.class, "0", "4");
	public static final EnumDpt<SABBehaviourLockUnlock> DPT_SABBehaviourLockUnlock = new EnumDpt<>(
			"20.802", "SABBehaviour Lock/Unlock", SABBehaviourLockUnlock.class, "0", "6");
	public static final EnumDpt<SSSBMode> DPT_SSSBMode = new EnumDpt<>("20.803", "SSSB Mode",
			SSSBMode.class, "1", "4");
	public static final EnumDpt<BlindsControlMode> DPT_BlindsControlMode = new EnumDpt<>("20.804",
			"Blinds Control Mode", BlindsControlMode.class, "0", "1");

	private static final Map<String, DPT> types;

	static {
		types = new HashMap<>();
		final Field[] fields = DPTXlator8BitEnum.class.getFields();
		for (int i = 0; i < fields.length; i++) {
			try {
				final Object o = fields[i].get(null);
				if (o instanceof DPT) {
					final DPT dpt = (DPT) o;
					types.put(dpt.getID(), dpt);
				}
			}
			catch (final IllegalAccessException e) {}
		}
	}

	/**
	 * Creates a translator for the given datapoint type.
	 * <p>
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
	 * <p>
	 *
	 * @param dptID available implemented datapoint type ID
	 * @throws KNXFormatException on wrong formatted or not expected (available) <code>dptID</code>
	 */
	public DPTXlator8BitEnum(final String dptID) throws KNXFormatException
	{
		super(1);
		setTypeID(types, dptID);
		final EnumSet<? extends EnumBase<?>> set = EnumSet.allOf(((EnumDpt<?>) dpt).elements);
		data = new short[] { (short) set.iterator().next().value() };
	}

	/* (non-Javadoc)
	 * @see tuwien.auto.calimero.dptxlator.DPTXlator#getValue()
	 */
	public String getValue()
	{
		return makeString(0);
	}

	/**
	 * Sets one new translation item from an unsigned value, replacing any old items.
	 * <p>
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
	 * Returns the first translation item.
	 * <p>
	 *
	 * @return element as unsigned 8 Bit using type short
	 * @throws KNXFormatException
	 * @see #getType()
	 */
	public final short getValueUnsigned() throws KNXFormatException
	{
		return fromDPT(0);
	}

	/**
	 * Returns the first translation item.
	 *
	 * @return numeric value of element
	 * @throws KNXFormatException
	 * @see tuwien.auto.calimero.dptxlator.DPTXlator#getNumericValue()
	 * @see #getValueUnsigned()
	 */
	public final double getNumericValue() throws KNXFormatException
	{
		return getValueUnsigned();
	}

	/* (non-Javadoc)
	 * @see tuwien.auto.calimero.dptxlator.DPTXlator#getAllValues()
	 */
	public String[] getAllValues()
	{
		final String[] s = new String[data.length];
		for (int i = 0; i < data.length; ++i)
			s[i] = makeString(i);
		return s;
	}

	/* (non-Javadoc)
	 * @see tuwien.auto.calimero.dptxlator.DPTXlator#getSubTypes()
	 */
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
				logThrow(LogLevel.WARN, "value '" + value + "' is no element of "
						+ ((EnumDpt<?>) dpt).elements.getSimpleName() + " enumeration", null, value);
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
		final short v = data[0];
		validate(v);
		return v;
	}

	private void validate(final int value) throws KNXFormatException
	{
		final EnumDpt<?> enumDpt = (EnumDpt<?>) dpt;
		if (!enumDpt.contains(value))
			logThrow(LogLevel.WARN,
					"value " + value + " is no element of " + enumDpt.elements.getSimpleName()
							+ " enumeration", null, Integer.toString(value));
	}
}
