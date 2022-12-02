# Raspberry Pi Pico Programmable IO Low Speed USB RX Simulation

This repository contains a resources demonstrating simulation of low speed USB reception.

Using the simulator rp2040pio: https://github.com/soundpaint/rp2040pio

With documentation: https://rp2040pio-docs.readthedocs.io/

## Files

* FIFODumper.java - Small utility which dumps the PIO RX fifo to a file. You can read the fifo directly in the simulator, but for lager packets I've found having it in a file is useful.
* saleae_logic_csv_to_sim_script.py - Original data was captured with a Saleae Logic analyser and exported to a CSV file. This tool converts it to a simulation script.
* usb_packet_saleae.csv - Example capture from a cheap keyboard. Saleae Logic export of D- channel.
* usb_packet.sim - Result of saleae_logic_csv_to_sim_script.py ran against usb_packet_saleae.csv.
* usb_rx.mon - Simulation script, configuring the virtual PIO module and feeding in the simulation data.
* usb_rx.pio - PIO program to run within SM

## Usage

1. Clone simulator

    ```
    git clone git@github.com:soundpaint/rp2040pio.git
    ```

    If using irq wait instruction you may find a bug, see my branch: https://github.com/pgreenland/rp2040pio/tree/irq_wait - should really submit a PR

2. Ensure pre-requisites available, docs list:

    * Java OpenJDK 11.x (other recent versions should also work, but have not been tested)
        * Debian: `apt-get install openjdk-11-jdk`
    * GNU Make

3. Build rp2040pio

    ```
    pushd rp2040pio
    make all
    popd
    ```

4. Build simple tool to extract and store any data from rx fifo before its overwritten

    ```
    javac --source-path "rp2040pio/java" FIFODumper.java
    ```

5. Assemble PIO program. 

    The pioasm binary is built during a pico build, I've previously dug it out of the build directory for a random project which uses pio.

    Seems you can build it directly:

    ```
    git clone https://github.com/raspberrypi/pico-sdk.git
    cd pico-sdk/tools/pioasm
    mkdir build
    cd build
    cmake ..
    make
    ```

    However its acquired, assemble the usb_rx.pio program:

    ```
    pioasm -o hex usb_rx.pio usb_rx.hex
    ```

6. Launch simulation server

    ```
    java -jar rp2040pio/jar/rp2040pio_server.jar &
    ```

7. Launch displays

    ```
    java -jar rp2040pio/jar/rp2040pio_gpioobserver.jar &
    java -jar rp2040pio/jar/rp2040pio_codeobserver.jar &
    java -jar rp2040pio/jar/rp2040pio_diagram.jar &
    ```

8. Launch fifo dumper

    ```
    java -Xdiag -classpath "rp2040pio/java:." FIFODumper > fifo.log &
    ```

9. Launch simulation script in monitor

    ```
    java -jar rp2040pio/jar/rp2040pio_monitor.jar -f usb_rx.mon &
    ```

10. Run simulation

    Either hitting the emulate x cycles button in the "Diagram Creator" window.

    Or launch another monitor instance:
    ```
    java -jar rp2040pio/jar/rp2040pio_monitor.jar
    ```

    and execute a number of cycles:
    ```
    trace -c 2000
    ```
