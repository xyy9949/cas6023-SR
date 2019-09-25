## Content:

This repository contains Cassandra version 2.0.0 which reproduces the bug Cas-6023. 

The code is cloned from [DMCK](https://github.com/ucare-uchicago/DMCK/tree/master/dmck-target-systems) model checker's target systems which instruments the code to work together with a model checker. The model checker intercepts the messages in the system and delivers them in a specific order.

This repository modifies the instrumentation so that the Cassandra nodes communicate to a model checker or a tester via network. 

## Requirements:


- Java 7

- Ant 1.9.14


## Compilation:

```
ant
```

For more details, please refer to README.txt.



## Note: 
Installation on a VM is suggested since the software depends on the old versions of the libraries.
