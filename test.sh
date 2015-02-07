#!/bin/bash 

groovysh -cp $GATE_HOME/bin/gate.jar:./gate-lib/'*':Evaluation.jar -e ':load init.groovy'
