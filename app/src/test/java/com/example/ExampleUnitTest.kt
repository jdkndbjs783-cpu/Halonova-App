package com.example

import com.example.service.CommandResult
import com.example.service.SmartActionType
import com.example.service.VoiceCommandParser
import org.junit.Assert.*
import org.junit.Test

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun commandParser_matchesSettingsCommand() {
    val parser = VoiceCommandParser()
    val result = parser.parse("Open Settings")
    assertTrue(result is CommandResult.ActionCommand)
    assertEquals(SmartActionType.OPEN_SETTINGS, (result as CommandResult.ActionCommand).actionType)
  }

  @Test
  fun commandParser_matchesWifiCommand() {
    val parser = VoiceCommandParser()
    val result = parser.parse("Open Wi-Fi")
    assertTrue(result is CommandResult.ActionCommand)
    assertEquals(SmartActionType.OPEN_WIFI, (result as CommandResult.ActionCommand).actionType)
  }

  @Test
  fun commandParser_matchesBluetoothCommand() {
    val parser = VoiceCommandParser()
    val result = parser.parse("Open Bluetooth")
    assertTrue(result is CommandResult.ActionCommand)
    assertEquals(SmartActionType.OPEN_BLUETOOTH, (result as CommandResult.ActionCommand).actionType)
  }

  @Test
  fun commandParser_matchesBatteryCommand() {
    val parser = VoiceCommandParser()
    val result = parser.parse("Open Battery Settings")
    assertTrue(result is CommandResult.ActionCommand)
    assertEquals(SmartActionType.OPEN_BATTERY, (result as CommandResult.ActionCommand).actionType)
  }

  @Test
  fun commandParser_matchesLocationCommand() {
    val parser = VoiceCommandParser()
    val result = parser.parse("Open Location Settings")
    assertTrue(result is CommandResult.ActionCommand)
    assertEquals(SmartActionType.OPEN_LOCATION, (result as CommandResult.ActionCommand).actionType)
  }

  @Test
  fun commandParser_handlesUnknownPhrase() {
    val parser = VoiceCommandParser()
    val result = parser.parse("Tell me a dark cyber story")
    assertTrue(result is CommandResult.UnknownCommand)
    assertEquals("Tell me a dark cyber story", (result as CommandResult.UnknownCommand).rawInput)
  }
}
