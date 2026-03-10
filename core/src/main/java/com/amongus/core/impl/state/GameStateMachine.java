package com.amongus.core.impl.state;

import com.amongus.core.api.state.GameState;

public class GameStateMachine {

    private GameState currentState; //  Estado actual...

    public GameStateMachine(){
        this.currentState = GameState.LOBBY; //Se accede al enum en api/state/GameState
    }

    public GameState getCurrentState(){
        return currentState;
    }

    //Transicion entre estados, de lobby a juego...
    public void transitionTo(GameState nextState){
        if(!validTransition(currentState, nextState)) {
            throw new IllegalArgumentException(
                    "Invalid state transition: " + currentState + " -> " + nextState
            );
        }
        this.currentState = nextState;
    }


    private boolean validTransition(GameState from, GameState to){
        return switch (from) {
            case LOBBY -> to == GameState.IN_GAME;
            case IN_GAME -> to == GameState.MEETING || to == GameState.ENDED;
            case MEETING -> to == GameState.IN_GAME || to == GameState.ENDED;
            case ENDED -> false;
        };
    }
    public void reset() {
        this.currentState = GameState.LOBBY;
    }
}






