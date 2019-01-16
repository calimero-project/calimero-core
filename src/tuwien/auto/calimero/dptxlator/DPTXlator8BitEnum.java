/*
    Calimero 2 - A library for KNX network access
    Copyright (c) 2015, 2019 B. Malinowsky

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

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

import tuwien.auto.calimero.KNXFormatException;
import tuwien.auto.calimero.KNXIllegalArgumentException;

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

	// HVAC Command field (Z8): 8 bit enumeration value in case of a write service
	enum HvacCommand {
		NormalWrite, Override, Release, SetOutOfService, ResetOutOfService, AlarmAck, SetToDefault
	}

	// use a common base to store and access enumeration elements independent of specific type
	private interface EnumBase<E extends EnumBase<E>>
	{
		Map<EnumBase<?>, Integer> values = new HashMap<>();
		Map<EnumBase<?>, String> descriptions = new HashMap<>();

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

	public enum SystemClockMode implements EnumBase<SystemClockMode> {
		Autonomous(0, "autonomous"),
		Slave(1, "slave"),
		Master(2, "master");

		SystemClockMode(final int element, final String description)
		{ //RP2
			init(element, description);
		} //RP1
	}

	public enum BuildingMode implements EnumBase<BuildingMode> {
		BuildingInUse(0, "Building in use"),
		BuildingNotUsed(1, "Building not used"),
		BuildingProtection(2, "Building protection");

		BuildingMode(final int element, final String description)
		{ //RP2
			init(element, description);
		} //RP1
	}

	public enum OccupancyMode implements EnumBase<OccupancyMode> {
		Occupied(0, "occupied"),
		Standby(1, "standby"),
		NotOccupied(2, "not occupied");

		OccupancyMode(final int element, final String description)
		{ //RP2
			init(element, description);
		}
		//RP1
	}

	public enum Priority implements EnumBase<Priority> {
		High(0, "High"),
		Medium(1, "Medium"),
		Low(2, "Low"),
		Void(3, "void");

		Priority(final int element, final String description)
		{ //RP2
			init(element, description);
		}
		//RP1
	}

	public enum LightApplicationMode implements EnumBase<LightApplicationMode> {
		Normal(0, "normal"),
		PresenceSimulation(1, "presence simulation"),
		NightRound(2, "night round");

		LightApplicationMode(final int element, final String description)
		{ //RP2
			init(element, description);
		}
		//RP1
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

		ApplicationArea(final int element, final String description)
		{ //RP2
			init(element, description);
		}
		//RP1
	}

	public enum AlarmClassType implements EnumBase<AlarmClassType> {
		SimpleAlarm(1, "simple alarm"),
		BasicAlarm(2, "basic alarm"),
		ExtendedAlarm(3, "extended alarm");

		AlarmClassType(final int element, final String description)
		{ //RP2
			init(element, description);
		} //RP1
	}

	public enum PsuMode implements EnumBase<PsuMode> {
		Disabled(0, "disabled (PSU/DPSU fixed off)"),
		Enabled(1, "enabled (PSU/DPSU fixed on)"),
		Auto(2, "auto (PSU/DPSU automatic on/off)");

		PsuMode(final int element, final String description)
		{ //RP2
			init(element, description);
		} //RP1
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

		ErrorClassSystem(final int element, final String description)
		{ //RP2
			init(element, description);
		} //RP1
	}

	public enum ErrorClassHvac implements EnumBase<ErrorClassHvac> {
		NoFault(0, "no fault"),
		SensorFault(1, "sensor fault"),
		ProcessFault(2, "process fault / controller fault"),
		ActuatorFault(3, "actuator fault"),
		OtherFault(4, "other fault");

		ErrorClassHvac(final int element, final String description)
		{ //RP2
			init(element, description);
		} //RP1
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

		TimeDelay(final int element, final String description)
		{ //RP2
			init(element, description);
		} //RP1
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

		BeaufortWindForceScale(final int element, final String description)
		{ //RP2
			init(element, description);
		} //RP1
	}

	public enum SensorSelect implements EnumBase<SensorSelect> {
		Inactive(0, "inactive"),
		DigitalInput(1, "digital input (not inverted)"),
		DigitalInputInverted(2, "digital input inverted"),
		AnalogInput(3, "analog input, 0 % to 100 %"),
		TemperatureSensorInput(4, "temperature sensor input");

		SensorSelect(final int element, final String description)
		{ //RP2
			init(element, description);
		} //RP1
	}

	public enum ActuatorConnectType implements EnumBase<ActuatorConnectType> {
		SensorConnection(1, "Sensor Connection"),
		ControllerConnection(2, "Controller Connection");

		ActuatorConnectType(final int element, final String description)
		{ //RP2
			init(element, description);
		} //RP1
	}

	public enum FuelType implements EnumBase<FuelType> {
		Auto(0, "auto"),
		Oil(1, "oil"),
		Gas(2, "gas"),
		SolidStateFuel(3, "solid state fuel");

		FuelType(final int element, final String description)
		{ //RP2
			init(element, description);
		} //RP1
	}

	public enum BurnerType implements EnumBase<BurnerType> {
		OneStage(1, "1 stage"),
		TwoStage(2, "2 stage"),
		Modulating(3, "modulating");

		BurnerType(final int element, final String description)
		{ //RP2
			init(element, description);
		} //RP1
	}

	public enum HvacMode implements EnumBase<HvacMode> {
		Auto(0, "Auto"),
		Comfort(1, "Comfort"),
		Standby(2, "Standby"),
		Economy(3, "Economy"),
		BuildingProtection(4, "Building Protection");

		HvacMode(final int element, final String description)
		{ //RP2
			init(element, description);
		} //RP1
	}

	/** Domestic Hot Water Mode. */
	public enum DomesticHotWaterMode implements EnumBase<DomesticHotWaterMode> {
		Auto(0, "Auto"),
		LegioProtect(1, "Legio Protect"),
		Normal(2, "Normal"),
		Reduced(3, "Reduced"),
		OffOrFrostProtect(4, "Off / Frost Protect");

		DomesticHotWaterMode(final int element, final String description)
		{ //RP2
			init(element, description);
		} //RP1
	}

	public enum LoadPriority implements EnumBase<LoadPriority> {
		None(0, "None"),
		ShiftLoadPriority(1, "Shift load priority"),
		AbsoluteLoadPriority(2, "Absolute load priority");

		LoadPriority(final int element, final String description)
		{ //RP2
			init(element, description);
		} //RP1
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

		HvacControlMode(final int element, final String description)
		{ //RP2
			init(element, description);
		} //RP1
	}

	public enum HvacEmergencyMode implements EnumBase<HvacEmergencyMode> {
		Normal(0, "Normal"),
		Pressure(1, "Emergency Pressure"),
		Depressure(2, "Emergency Depressure"),
		Purge(3, "Emergency Purge"),
		Shutdown(4, "Emergency Shutdown"),
		Fire(5, "Emergency Fire");

		HvacEmergencyMode(final int element, final String description)
		{ //RP2
			init(element, description);
		} //RP1
	}

	public enum ChangeoverMode implements EnumBase<ChangeoverMode> {
		Auto(0, "Auto"),
		CoolingOnly(1, "Cooling Only"),
		HeatingOnly(2, "Heating Only");

		ChangeoverMode(final int element, final String description)
		{ //RP2
			init(element, description);
		} //RP1
	}

	public enum ValveMode implements EnumBase<ValveMode> {
		HeatStageA(1, "Heat stage A for normal heating"),
		HeatStageB(2, "Heat stage B for heating with two stages (A + B)"),
		CoolStageA(3, "Cool stage A for normal cooling"),
		CoolStageB(4, "Cool stage B for cooling with two stages (A + B)"),
		HeatCool(5, "Heat/Cool for changeover applications");

		ValveMode(final int element, final String description)
		{ //RP2
			init(element, description);
		} //RP1
	}

	public enum DamperMode implements EnumBase<DamperMode> {
		FreshAir(1, "Fresh air, e.g., fancoils"),
		SupplyAir(2, "Supply Air, e.g. Variable Air Volume (VAV)"),
		ExtractAir(3, "Extract Air, e.g. Variable Air Volume (VAV)"),
		ExtractAir2(4, "Extract Air, e.g. Variable Air Volume (VAV)");

		DamperMode(final int element, final String description)
		{ //RP2
			init(element, description);
		} //RP1
	}

	public enum HeaterMode implements EnumBase<HeaterMode> {
		HeatStageA(1, "Heat Stage A On/Off"),
		HeatStageAProportional(2, "Heat Stage A Proportional"),
		HeatStageBProportional(3, "Heat Stage B Proportional");

		HeaterMode(final int element, final String description)
		{ //RP2
			init(element, description);
		} //RP1
	}

	public enum FanMode implements EnumBase<FanMode> {
		NotRunning(0, "not running"),
		PermanentlyRunning(1, "permanently running"),
		Intervals(2, "running in intervals");

		FanMode(final int element, final String description)
		{ //RP2
			init(element, description);
		} //RP1
	}

	public enum MasterSlaveMode implements EnumBase<MasterSlaveMode> {
		Autonomous(0, "autonomous"),
		Master(1, "master"),
		Slave(2, "slave");

		MasterSlaveMode(final int element, final String description)
		{ //RP2
			init(element, description);
		} //RP1
	}

	public enum StatusRoomSetpoint implements EnumBase<StatusRoomSetpoint> {
		Normal(0, "normal setpoint"),
		Alternative(1, "alternative setpoint"),
		BuildingProtection(2, "building protection setpoint");

		StatusRoomSetpoint(final int element, final String description)
		{ //RP2
			init(element, description);
		} //RP1
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

		MeteringDeviceType(final int element, final String description)
		{ //RP2
			init(element, description);
		} //RP1
	}

	public enum AirDamperActuatorType implements EnumBase<AirDamperActuatorType> {

		AirDamper(1, "Air Damper"),
		Vav(2, "Variable Air Volume");

		AirDamperActuatorType(final int element, final String description)
		{ //RP2
			init(element, description);
		} //RP1
	}

	public enum BackupMode implements EnumBase<BackupMode> {
		BackupValue(0, "Backup Value"),
		KeepLastState(1, "Keep Last State");

		BackupMode(final int element, final String description)
		{ //RP2
			init(element, description);
		} //RP1
	}

	public enum StartSynchronization implements EnumBase<StartSynchronization> {
		PositionUnchanged(0, "Position unchanged"),
		SingleClose(1, "Single close"),
		SingleOpen(2, "Single open");

		StartSynchronization(final int element, final String description)
		{ //RP2
			init(element, description);
		} //RP1
	}

	public enum BehaviorLockUnlock implements EnumBase<BehaviorLockUnlock> {
		Off(0, "off"),
		On(1, "on"),
		NoChange(2, "no change"),
		AdditionalParameter(3, "value according additional parameter"),
		MemoryFunction(4, "memory function value"),
		UpdatedValue(5, "updated value"),
		ValueBeforeLocking(6, "value before locking");

		BehaviorLockUnlock(final int element, final String description)
		{ //RP2
			init(element, description);
		} //RP1
	}

	public enum BehaviorBusPowerUpDown implements EnumBase<BehaviorBusPowerUpDown> {
		Off(0, "off"),
		On(1, "on"),
		NoChange(2, "no change"),
		AdditionalParameter(3, "value according additional parameter"),
		Last(4, "last (value before bus power down)");

		BehaviorBusPowerUpDown(final int element, final String description)
		{ //RP2
			init(element, description);
		} //RP1
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

		DaliFadeTime(final int element, final String description)
		{ //RP2
			init(element, description);
		} //RP1
	}

	public enum BlinkingMode implements EnumBase<BlinkingMode> {
		Disabled(0, "Blinking Disabled"),
		WithoutAcknowledge(1, "Without Acknowledge"),
		WithAcknowledge(2, "Blinking With Acknowledge ");

		BlinkingMode(final int element, final String description)
		{ //RP2
			init(element, description);
		} //RP1
	}

	public enum LightControlMode implements EnumBase<LightControlMode> {
		Automatic(0, "automatic light control"),
		Manual(1, "manual light control");

		LightControlMode(final int element, final String description)
		{ //RP2
			init(element, description);
		} //RP1
	}

	public enum SwitchPBModel implements EnumBase<SwitchPBModel> {
		OnePB(1, "one PB/binary input mode"),
		TwoPBs(2, "two PBs/binary inputs mode");

		SwitchPBModel(final int element, final String description)
		{ //RP2
			init(element, description);
		} //RP1
	}

	public enum SwitchPBAction implements EnumBase<SwitchPBAction> {
		Inactive(0, "inactive (no message sent)"),
		SwitchOffSent(1, "Switch-Off message sent"),
		SwitchOnSent(2, "Switch-On message sent"),
		InfoOnOff(3, "inverse value of Info On/Off is sent");

		SwitchPBAction(final int element, final String description)
		{ //RP2
			init(element, description);
		} //RP1
	}

	/** Light Dimming Sensor Basic Mode. */
	public enum LdsbMode implements EnumBase<LdsbMode> {
		OnePushButton(1, "one push-button/binary input, Switch On/Off inverts on each transmission"),
		OnePushButtonDimUp(2, "one push-button/binary input, On/Dim-Up message sent"),
		OnePushButtonDimDown(3, "one push-button/binary input, Off/Dim-Down message sent"),
		TwoPushButtons(4, "two push-buttons/binary inputs mode");

		LdsbMode(final int element, final String description)
		{ //RP2
			init(element, description);
		} //RP1
	}

	public enum SwitchOnMode implements EnumBase<SwitchOnMode> {
		LastActualValue(0, "last actual value"),
		AdditionalParameter(1, "value according additional parameter"),
		LastReceivedSetvalue(2, "last received absolute setvalue");

		SwitchOnMode(final int element, final String description)
		{ //RP2
			init(element, description);
		} //RP1
	}

	public enum LoadTypeSet implements EnumBase<LoadTypeSet> {
		Automatic(0, "automatic"),
		LeadingEdge(1, "leading edge (inductive load)"),
		TrailingEdge(2, "trailing edge (capacitive load)");

		LoadTypeSet(final int element, final String description)
		{ //RP2
			init(element, description);
		} //RP1
	}

	public enum LoadTypeDetected implements EnumBase<LoadTypeDetected> {
		Undefined(0, "undefined"),
		LeadingEdge(1, "leading edge (inductive load)"),
		TrailingEdge(2, "trailing edge (capacitive load)"),
		DetectionNotPossible(3, "detection not possible or error");

		LoadTypeDetected(final int element, final String description)
		{ //RP2
			init(element, description);
		} //RP1
	}

	/** Sunblind Actuator Basic Exception Behavior. */
	public enum SabExceptBehavior implements EnumBase<SabExceptBehavior> {
		Up(0, "up"),
		Down(1, "down"),
		NoChange(2, "no change"),
		AdditionalParameter(3, "value according additional parameter"),
		Stop(4, "stop");

		SabExceptBehavior(final int element, final String description)
		{ //RP2
			init(element, description);
		} //RP1
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

		SabBehaviorLockUnlock(final int element, final String description)
		{ //RP2
			init(element, description);
		} //RP1
	}

	/** Sunblind Sensor Basic Mode. */
	public enum SssbMode implements EnumBase<SssbMode> {
		OnePushButton(1, "one push button/binary input: Move-Up/Down inverts on each transmission"),
		OnePushButtonUp(2, "one push button/binary input: Move-Up/Step-Up message sent"),
		OnePushButtonDown(3, "one push button/binary input: Move-Down/Step-Down message sent"),
		TwoPushButtons(4, "two push buttons/binary inputs mode");

		SssbMode(final int element, final String description)
		{ //RP2
			init(element, description);
		} //RP1
	}

	public enum BlindsControlMode implements EnumBase<BlindsControlMode> {
		AutomaticControl(0, "Automatic Control"),
		ManualControl(1, "Manual Control");

		BlindsControlMode(final int element, final String description)
		{ //RP2
			init(element, description);
		} //RP1
	}

	// 20.1000
	public enum CommMode implements EnumBase<CommMode> {
		DataLinkLayer(0, "Data link layer"),
		Busmonitor(1, "Data link layer busmonitor"),
		RawFrames(2, "Data link layer raw frames"),
		cEmiTransportLayer(6, "cEMI transport layer"),
		NoLayer(0xff, "no layer");

		CommMode(final int element, final String description)
		{ //RP2
			init(element, description);
		} //RP1
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

		AddInfoType(final int element, final String description)
		{ //RP2
			init(element, description);
		} //RP1
	}

	// 20.1002
	public enum RFModeSelect implements EnumBase<RFModeSelect> {
		Asynchronous(0, "Asynchronous"),
		BiBatMaster(1, "Asynchronous + BiBat master"),
		BiBatSlave(2, "Asynchronous + BiBat slave");

		RFModeSelect(final int element, final String description)
		{ //RP2
			init(element, description);
		} //RP1
	}

	// 20.1003
	public enum RFFilterSelect implements EnumBase<RFFilterSelect> {
		None(0, "no filter"),
		DomainAddress(1, "filtering by DoA"),
		SerialNumber(2, "filtering by KNX serial number table"),
		DoAAndSN(3, "filtering by DoA and S/N table");

		RFFilterSelect(final int element, final String description)
		{ //RP2
			init(element, description);
		} //RP1
	}

	// 20.1200
	public enum MBusBreakerValveState implements EnumBase<MBusBreakerValveState> {
		Closed(0, "Breaker/Valve is closed"),
		Open(1, "Breaker/Valve is open"),
		Released(2, "Break/Valve is released"),
		Invalid(255, "invalid");

		MBusBreakerValveState(final int element, final String description)
		{ //RP2
			init(element, description);
		} //RP1
	}

	// 20.1202
	public enum GasMeasurementCondition implements EnumBase<GasMeasurementCondition> {
		Unknown(0, "unknown"),
		TemperatureConverted(1, "temperature converted"),
		BaseCondition(2, "at base condition"),
		MeasurementCondition(3, "at measurement condition");

		GasMeasurementCondition(final int element, final String description)
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
			final StringBuilder sb = new StringBuilder(30);
			sb.append(getID()).append(": ").append(getDescription()).append(", enumeration [");
			sb.append(getLowerValue()).append("..").append(getUpperValue()).append("]");
			return sb.toString();
		}
	}

	public static final EnumDpt<SystemClockMode> DptSystemClockMode = new EnumDpt<>("20.001", "System Clock Mode",
			SystemClockMode.class, "0", "2");
	public static final EnumDpt<BuildingMode> DptBuildingMode = new EnumDpt<>("20.002",
			"Building Mode", BuildingMode.class, "0", "2");
	public static final EnumDpt<OccupancyMode> DptOccupancyMode = new EnumDpt<>("20.003",
			"Occupancy Mode", OccupancyMode.class, "0", "2");
	public static final EnumDpt<Priority> DptPriority = new EnumDpt<>("20.004", "Priority",
			Priority.class, "0", "3");
	public static final EnumDpt<LightApplicationMode> DptLightApplicationMode = new EnumDpt<>(
			"20.005", "Light Application Mode", LightApplicationMode.class, "0", "2");
	public static final EnumDpt<ApplicationArea> DptApplicationArea = new EnumDpt<>("20.006",
			"Application Area", ApplicationArea.class, "0", "14");
	public static final EnumDpt<AlarmClassType> DptAlarmClassType = new EnumDpt<>("20.007",
			"Alarm Class Type", AlarmClassType.class, "0", "3");
	public static final EnumDpt<PsuMode> DptPsuMode = new EnumDpt<>("20.008", "PSU Mode",
			PsuMode.class, "0", "2");
	public static final EnumDpt<ErrorClassSystem> DptErrorClassSystem = new EnumDpt<>("20.011",
			"Error Class System", ErrorClassSystem.class, "0", "18");
	public static final EnumDpt<ErrorClassHvac> DptErrorClassHvac = new EnumDpt<>("20.012",
			"Error Class HVAC", ErrorClassHvac.class, "0", "4");
	public static final EnumDpt<TimeDelay> DptTimeDelay = new EnumDpt<>("20.013", "Time Delay",
			TimeDelay.class, "0", "25");
	public static final EnumDpt<BeaufortWindForceScale> DptBeaufortWindForceScale = new EnumDpt<>(
			"20.014", "Beaufort Wind Force Scale", BeaufortWindForceScale.class, "0", "12");
	public static final EnumDpt<SensorSelect> DptSensorSelect = new EnumDpt<>("20.017",
			"Sensor Select", SensorSelect.class, "0", "4");
	public static final EnumDpt<ActuatorConnectType> DptActuatorConnectType = new EnumDpt<>(
			"20.020", "Actuator Connect Type", ActuatorConnectType.class, "1", "2");
	public static final EnumDpt<FuelType> DptFuelType = new EnumDpt<>("20.100", "Fuel Type",
			FuelType.class, "0", "3");
	public static final EnumDpt<BurnerType> DptBurnerType = new EnumDpt<>("20.101", "Burner Type",
			BurnerType.class, "0", "3");
	public static final EnumDpt<HvacMode> DptHvacMode = new EnumDpt<>("20.102", "HVAC Mode",
			HvacMode.class, "0", "4");
	public static final EnumDpt<DomesticHotWaterMode> DptDomesticHotWaterMode = new EnumDpt<>("20.103", "DHW Mode",
			DomesticHotWaterMode.class, "0", "4");
	public static final EnumDpt<LoadPriority> DptLoadPriority = new EnumDpt<>("20.104",
			"Load Priority", LoadPriority.class, "0", "2");
	public static final EnumDpt<HvacControlMode> DptHvacControlMode = new EnumDpt<>("20.105",
			"HVAC Control Mode", HvacControlMode.class, "0", "20");
	public static final EnumDpt<HvacEmergencyMode> DptHvacEmergencyMode = new EnumDpt<>("20.106",
			"HVAC Emergency Mode", HvacEmergencyMode.class, "0", "5");
	public static final EnumDpt<ChangeoverMode> DptChangeoverMode = new EnumDpt<>("20.107",
			"Changeover Mode", ChangeoverMode.class, "0", "2");
	public static final EnumDpt<ValveMode> DptValveMode = new EnumDpt<>("20.108", "Valve Mode",
			ValveMode.class, "1", "5");
	public static final EnumDpt<DamperMode> DptDamperMode = new EnumDpt<>("20.109", "Damper Mode",
			DamperMode.class, "1", "4");
	public static final EnumDpt<HeaterMode> DptHeaterMode = new EnumDpt<>("20.110", "Heater Mode",
			HeaterMode.class, "1", "3");
	public static final EnumDpt<FanMode> DptFanMode = new EnumDpt<>("20.111", "Fan Mode",
			FanMode.class, "0", "2");
	public static final EnumDpt<MasterSlaveMode> DptMasterSlaveMode = new EnumDpt<>("20.112",
			"Master/Slave Mode", MasterSlaveMode.class, "0", "2");
	public static final EnumDpt<StatusRoomSetpoint> DptStatusRoomSetpoint = new EnumDpt<>("20.113",
			"Status Room Setpoint", StatusRoomSetpoint.class, "0", "2");
	public static final EnumDpt<MeteringDeviceType> DptMeteringDeviceType = new EnumDpt<>("20.114",
			"Metering Device Type", MeteringDeviceType.class, "0", "41/255");
	public static final EnumDpt<AirDamperActuatorType> DptAirDamperActuatorType = new EnumDpt<>("20.120",
			"Air Damper Actuator Type", AirDamperActuatorType.class, "1", "2");
	public static final EnumDpt<BackupMode> DptBackupMode = new EnumDpt<>("20.121", "Backup Mode",
			BackupMode.class, "0", "1");
	public static final EnumDpt<StartSynchronization> DptStartSynchronization = new EnumDpt<>(
			"20.122", "Start Synchronization", StartSynchronization.class, "0", "2");
	public static final EnumDpt<BehaviorLockUnlock> DptBehaviorLockUnlock = new EnumDpt<>(
			"20.600", "Behavior Lock/Unlock", BehaviorLockUnlock.class, "0", "6");
	public static final EnumDpt<BehaviorBusPowerUpDown> DptBehaviorBusPowerUpDown = new EnumDpt<>(
			"20.601", "Behavior Bus Power Up/Down", BehaviorBusPowerUpDown.class, "0", "4");
	/** DPT Digital Addressable Lighting Interface Fade Time. */
	public static final EnumDpt<DaliFadeTime> DptDaliFadeTime = new EnumDpt<>("20.602",
			"DALI Fade Time", DaliFadeTime.class, "0", "15");
	public static final EnumDpt<BlinkingMode> DptBlinkingMode = new EnumDpt<>("20.603",
			"Blinking Mode", BlinkingMode.class, "0", "2");
	public static final EnumDpt<LightControlMode> DptLightControlMode = new EnumDpt<>("20.604",
			"Light Control Mode", LightControlMode.class, "0", "1");
	public static final EnumDpt<SwitchPBModel> DptSwitchPBModel = new EnumDpt<>("20.605",
			"Switch PB Model", SwitchPBModel.class, "1", "2");
	public static final EnumDpt<SwitchPBAction> DptSwitchPBAction = new EnumDpt<>("20.606",
			"PB Action", SwitchPBAction.class, "0", "3");
	public static final EnumDpt<LdsbMode> DptDimmPBModel = new EnumDpt<>("20.607", "Dimm PB Model",
			LdsbMode.class, "1", "4");
	public static final EnumDpt<SwitchOnMode> DptSwitchOnMode = new EnumDpt<>("20.608",
			"Switch On Mode", SwitchOnMode.class, "0", "2");
	public static final EnumDpt<LoadTypeSet> DptLoadTypeSet = new EnumDpt<>("20.609",
			"Load Type Set", LoadTypeSet.class, "0", "2");
	public static final EnumDpt<LoadTypeDetected> DptLoadTypeDetected = new EnumDpt<>("20.610",
			"Load Type Detected", LoadTypeDetected.class, "0", "3");
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
			"Blinds Control Mode", BlindsControlMode.class, "0", "1");

	// System domain

	public static final EnumDpt<CommMode> DptCommMode = new EnumDpt<>("20.1000", "Comm Mode",
			CommMode.class, "0", "255");
	public static final EnumDpt<AddInfoType> DptAddInfoType = new EnumDpt<>("20.1001",
			"Additional Info Type", AddInfoType.class, "0", "7");
	public static final EnumDpt<RFModeSelect> DptRFModeSelect = new EnumDpt<>("20.1002",
			"RF Mode Select", RFModeSelect.class, "0", "2");
	public static final EnumDpt<RFFilterSelect> DptRFFilterSelect = new EnumDpt<>("20.1003",
			"RF Filter Select", RFFilterSelect.class, "0", "3");

	// Metering domain

	public static final EnumDpt<MBusBreakerValveState> DptMBusBreakerValveState = new EnumDpt<>(
			"20.1200", "M-Bus Breaker/Valve State", MBusBreakerValveState.class, "0", "255");
	public static final EnumDpt<GasMeasurementCondition> DptGasMeasurementCondition = new EnumDpt<>(
			"20.1202", "Gas Measurement Condition", GasMeasurementCondition.class, "0", "3");

	private static final Map<String, DPT> types = loadDatapointTypes(DPTXlator8BitEnum.class);

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
		final EnumSet<? extends EnumBase<?>> set = EnumSet.allOf(((EnumDpt<?>) dpt).elements);
		data = new short[] { (short) set.iterator().next().value() };
	}

	@Override
	public String getValue()
	{
		return makeString(0);
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
						+ ((EnumDpt<?>) dpt).elements.getSimpleName() + " enumeration", value);
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
			throw newException("value is no element of " + enumDpt.elements.getSimpleName()
					+ " enumeration", Integer.toString(value));
	}
}
