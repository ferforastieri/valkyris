package com.ferforastieri.camtacte

import com.ferforastieri.camtacte.core.model.Rule
import com.ferforastieri.camtacte.core.model.RuleActions
import org.junit.Assert.assertTrue
import org.junit.Test

class ModelTest {
    @Test fun alarmAndRecordingCanBeEnabledTogether(){val rule=Rule(cameraId="camera",name="Baby cry",detectorTypes=listOf("baby_cry"),actions=RuleActions(record=true,notify=true,alarm=true));assertTrue(rule.actions.alarm&&rule.actions.record)}
}

