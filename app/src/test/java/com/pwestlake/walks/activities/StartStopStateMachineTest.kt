package com.pwestlake.walks.activities

import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class StartStopStateMachineTest {
    var stateMachine: StartStopStateMachine? = null

    @Before
    fun init() {
        stateMachine = StartStopStateMachine()
    }

    @Test
    fun testInitialState() {
        assertEquals(stopped, stateMachine!!.currentState)
    }

    @Test
    fun testRun() {
        stateMachine!!.nextState(run)
        assertEquals(running, stateMachine!!.currentState)
    }

    @Test
    fun testRunningStop() {
        stateMachine!!.nextState(run)
        stateMachine!!.nextState(stop)
        assertEquals(stopped, stateMachine!!.currentState)
    }

    @Test
    fun testRunningPause() {
        stateMachine!!.nextState(run)
        stateMachine!!.nextState(pause)
        assertEquals(paused, stateMachine!!.currentState)
    }

    @Test
    fun testPausedRun() {
        stateMachine!!.nextState(run)
        stateMachine!!.nextState(pause)
        stateMachine!!.nextState(run)
        assertEquals(running, stateMachine!!.currentState)
    }

    @Test
    fun testPausedStop() {
        stateMachine!!.nextState(run)
        stateMachine!!.nextState(pause)
        stateMachine!!.nextState(stop)
        assertEquals(stopped, stateMachine!!.currentState)
    }
}