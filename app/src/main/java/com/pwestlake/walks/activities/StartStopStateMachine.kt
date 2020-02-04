package com.pwestlake.walks.activities

import androidx.databinding.BaseObservable
import androidx.databinding.Bindable
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.pwestlake.walks.BR

sealed class Transition(val name: String)
object run: Transition("Run")
object stop: Transition("Stop")
object pause: Transition("Pause")

sealed class State(val name: String, val transitions: Set<Transition>)
object running: State("Running", setOf(stop, pause))
object stopped: State("Stopped", setOf(run))
object paused: State("Paused", setOf(run, stop))

class StartStopStateMachine: BaseObservable(){
    val states = setOf(running, stopped, paused)
    var currentState: State = stopped

    @get:Bindable
    var runEnabled: Int = FloatingActionButton.VISIBLE
        set(value) {
            field = value
            notifyPropertyChanged(BR.runEnabled)
        }

    @get:Bindable
    var pauseEnabled: Int = FloatingActionButton.INVISIBLE
        set(value) {
            field = value
            notifyPropertyChanged(BR.pauseEnabled)
        }

    @get:Bindable
    var stopEnabled: Int = FloatingActionButton.INVISIBLE
        set(value) {
            field = value
            notifyPropertyChanged(BR.stopEnabled)
        }

    fun nextState(transition: Transition): State {
        var state: State = currentState

        when (transition) {
            run -> {
                when (currentState) {
                    stopped -> state = running
                    paused -> state = running
                }

                runEnabled = FloatingActionButton.INVISIBLE
                pauseEnabled = FloatingActionButton.VISIBLE
                stopEnabled = FloatingActionButton.VISIBLE
            }

            stop -> {
                when (currentState) {
                    running -> state = stopped
                    paused -> state = stopped
                }

                runEnabled = FloatingActionButton.VISIBLE
                stopEnabled = FloatingActionButton.INVISIBLE
                pauseEnabled = FloatingActionButton.INVISIBLE
            }

            pause -> {
                when (currentState) {
                    running -> state = paused
                }

                runEnabled = FloatingActionButton.VISIBLE
                pauseEnabled = FloatingActionButton.INVISIBLE
            }
        }

        currentState = state
        return currentState
    }

    fun isRunEnabled(): Boolean {
        return currentState == stopped || currentState == paused
    }
}