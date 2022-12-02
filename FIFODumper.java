import java.io.PrintStream;
import java.io.IOException;

import org.soundpaint.rp2040pio.Constants;
import org.soundpaint.rp2040pio.PicoEmuRegisters;
import org.soundpaint.rp2040pio.PIOEmuRegisters;
import org.soundpaint.rp2040pio.PIORegisters;
import org.soundpaint.rp2040pio.RemoteAddressSpaceClient;
import org.soundpaint.rp2040pio.sdk.SDK;

public class FIFODumper {
	private final RemoteAddressSpaceClient sdkClient;
	private final SDK sdk;

	public FIFODumper(PrintStream console) throws IOException {
		int port = Constants.REGISTER_SERVER_DEFAULT_PORT_NUMBER;
		console.printf("connecting to emulation server at port %d…%n", port);
		sdkClient = new RemoteAddressSpaceClient(console);
		sdk = new SDK(console, sdkClient);
		sdkClient.connect(null, port);

		final int addressPhase0 = PicoEmuRegisters.getAddress(PicoEmuRegisters.Regs.MASTERCLK_TRIGGER_PHASE0);
		final int addressPhase1 = PicoEmuRegisters.getAddress(PicoEmuRegisters.Regs.MASTERCLK_TRIGGER_PHASE1);
		final int expectedValue = 0x1; // update upon stable cycle phase 1
		final int mask = 0xffffffff;
		final int cyclesTimeout = 0;
		final int refresh = 500;
		final int millisTimeoutPhase0 = refresh / 2;
		final int millisTimeoutPhase1 = refresh - millisTimeoutPhase0;

		final int pioNum = 0;
		final int smNum = 0;

		final int fifo_level_address = PIORegisters.getAddress(pioNum, PIORegisters.Regs.FLEVEL);
		final int fifo_read_address = PIORegisters.getRXFAddress(pioNum, smNum);

		while (true) {
			sdkClient.waitAddress(addressPhase1, expectedValue, mask,
								  cyclesTimeout, millisTimeoutPhase1);

			int fifo_level_rx = ((sdk.readAddress(fifo_level_address) >> (smNum << 3)) >> 4) & 0x0f;
			while (fifo_level_rx > 0) {
				int value = (sdk.readAddress(fifo_read_address) >> 24) & 0xff;
				//int value = sdk.readAddress(fifo_read_address) & 0xff;
				console.printf("dequeued %02x%n", value);
				fifo_level_rx--;
			}

			sdkClient.waitAddress(addressPhase0, expectedValue, mask,
								  cyclesTimeout, millisTimeoutPhase0);
		}
	}

	public static void main(String[] args) throws IOException {
		new FIFODumper(System.out);
	}
}
