import csv

with open("usb_packet_saleae.csv", "r") as input:
	with open("usb_packet.sim", "w") as output:
		# Init CSV reader
		reader = csv.reader(input, delimiter=',')

		# Skip header row
		next(reader, None)

		# Iterate over data rows
		idle_entry_found = False
		last_time = 0
		last_val = 0
		total_cycles = 0
		for row in reader:
			# Split row
			time, val = row

			# Convert fields
			time = float(time)
			val = bool(int(val))

			# Look for first entry with line idle
			if not idle_entry_found:
				if not val:
					# Looking for idle entry and this isn't it, continue
					continue

				# Found idle entry, update last values
				last_time = time
				last_val = val

				# Flag entry found
				idle_entry_found = True

				# Skip entry
				continue

			# Calculate delay from previous value
			delay_secs = time - last_time

			# Convert from seconds to 16 * cycles
			delay_cycles = round((16 * delay_secs) / (1 / 1500000))

			# Print summary
			summmary = "Time: {} - Val: {} - Delay MicroSecs: {} - Delay Cycles: {} - Start Cycle: {} - End Cycle: {}".format(last_time, last_val, delay_secs * 1000000, delay_cycles, total_cycles, total_cycles + delay_cycles)
			print(summmary)
			output.write("# {}\n".format(summmary))

			# Output value
			output.write("gpio --gpio=0 {}\n".format("--set" if last_val else "--clear"))

			# Output delay
			# Address and value parameters are required but we don't actually care
			# cycles are what are important here.
			# The docs: https://rp2040pio-docs.readthedocs.io/en/latest/pico-emu-registers.html
			# and: https://rp2040pio-docs.readthedocs.io/en/latest/pio-emu-registers.html
			# suggest that the register below is PWR_UP a emulator specific register which should never
			# be one
			output.write("wait --address=0x58000000 --value=0x1 --cycles {}\n\n".format(delay_cycles))

			# Update last values
			last_time = time
			last_val = val

			# Add to delay
			total_cycles += delay_cycles
