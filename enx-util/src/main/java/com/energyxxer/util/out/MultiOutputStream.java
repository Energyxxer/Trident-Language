package com.energyxxer.util.out;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;

public class MultiOutputStream extends OutputStream {
	private ArrayList<OutputStream> outputStreams;

	public MultiOutputStream(OutputStream... outputStreams) {
		this.outputStreams = new ArrayList<>(Arrays.asList(outputStreams));
	}

	@Override
	public void write(int b) throws IOException {
		for (OutputStream out : outputStreams)
			out.write(b);
	}

	@Override
	public void write(byte[] b) throws IOException {
		for (OutputStream out : outputStreams)
			out.write(b);
	}

	@Override
	public void write(byte[] b, int off, int len) throws IOException {
		for (OutputStream out : outputStreams)
			out.write(b, off, len);
	}

	@Override
	public void flush() throws IOException {
		for (OutputStream out : outputStreams)
			out.flush();
	}

	@Override
	public void close() throws IOException {
		for (OutputStream out : outputStreams)
			out.close();
	}

	public void addStream(OutputStream stream) {
		outputStreams.add(stream);
	}
}