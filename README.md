# TopoSync-SFC Demonstrator Code Repository
This repository contains the TopoSync-SFC demonstrator code.

## Prerequisites

* Ubuntu 18.04 LTS (other linux distros may work as well, but were not tested)
* ONOS SDN Controller (Version 1.13.6) 
* Gurobi ILP Solver (Version 8.1.0)
* Containernet v2.0 (using mininet Version 2.3.0d1) 
* Java 11
* Python 2.7.15+

Make sure to have all of the above installed and a valid Gurobi license set up (free for academic use).

## Run the demo
1. Start the ONOS controller on localhost.
2. Deploy the TopoSync-SFC SDN application. Use the bash script `toposync-app/buildAndDeploy.sh`.
3. Build the whack-a-mole applications (client and server) using `mvn package` in the `whack-a-mole-client` and `whack-a-mole-server` subdirectories.
4. Start the mininet instance with `sudo python mn/startup/start_topo.py paper`. 
Note that this step will run the demo in an fully emulated environment: both clients are emulated using containernet.
Executing this step will therefore open the ping plot as well as two windows displaying the whack-a-mole client view of both clients.

5. (optional) As alternative to 4., you can also use the hardware setup like described in the paper with `sudo python mn/startup/start_topo.py paper --hw` In this case, the whack-a-mole clients need to be manually started on the hardware devices.
6. Build the GUI which is supposed to run on the main laptop with `mvn package` in the `toposync-demo-gui` directory. Run it using `toposync-demo-gui/run.sh`. It will prompt for root priviliges, as dynamically changing link delays requires those.
