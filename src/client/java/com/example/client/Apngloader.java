package com.example.client;

import com.mojang.blaze3d.platform.NativeImage;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.CRC32;

public final class ApngLoader {
	public static final class Result {
		public final int width;
		public final int height;
		public final NativeImage[] frames;
		public final int[] delays;

		Result(int width, int height, NativeImage[] frames, int[] delays) {
			this.width = width;
			this.height = height;
			this.frames = frames;
			this.delays = delays;
		}
	}

	private static final class FrameInfo {
		int w, h, x, y, delayMs, dispose, blend;
		final ByteArrayOutputStream data = new ByteArrayOutputStream();
	}

	private static final long MAX_BYTES = 256L * 1024 * 1024;
	private static final byte[] SIG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A};

	private ApngLoader() {}

	/** Returns null if the data is not an animated PNG (or is too large). */
	public static Result load(byte[] data) throws IOException {
		if (data.length < 8) return null;
		for (int i = 0; i < 8; i++) {
			if (data[i] != SIG[i]) return null;
		}

		DataInputStream in = new DataInputStream(new ByteArrayInputStream(data, 8, data.length - 8));
		byte[] ihdr = null;
		boolean animated = false;
		ByteArrayOutputStream extra = new ByteArrayOutputStream();
		List<FrameInfo> infos = new ArrayList<>();
		FrameInfo cur = null;
		boolean done = false;

		while (!done) {
			int len;
			try {
				len = in.readInt();
			} catch (EOFException e) {
				break;
			}
			byte[] typeBytes = new byte[4];
			in.readFully(typeBytes);
			byte[] body = new byte[len];
			in.readFully(body);
			in.readInt(); // CRC, ignored
			String type = new String(typeBytes, StandardCharsets.US_ASCII);

			switch (type) {
				case "IHDR" -> ihdr = body;
				case "acTL" -> animated = true;
				case "fcTL" -> {
					cur = new FrameInfo();
					cur.w = readInt(body, 4);
					cur.h = readInt(body, 8);
					cur.x = readInt(body, 12);
					cur.y = readInt(body, 16);
					int num = ((body[20] & 255) << 8) | (body[21] & 255);
					int den = ((body[22] & 255) << 8) | (body[23] & 255);
					if (den == 0) den = 100;
					int ms = num * 1000 / den;
					cur.delayMs = ms < 20 ? 100 : ms;
					cur.dispose = body[24];
					cur.blend = body[25];
					infos.add(cur);
				}
				case "IDAT" -> {
					if (cur != null) cur.data.write(body, 0, body.length);
				}
				case "fdAT" -> {
					if (cur != null && body.length > 4) cur.data.write(body, 4, body.length - 4);
				}
				case "PLTE", "tRNS" -> writeChunk(extra, type, body);
				case "IEND" -> done = true;
				default -> { }
			}
		}

		if (!animated || ihdr == null || infos.size() < 2) return null;

		int cw = readInt(ihdr, 0);
		int ch = readInt(ihdr, 4);
		long bytes = (long) cw * ch * 4L * infos.size();
		if (bytes > MAX_BYTES) {
			System.out.println("[Yskins] APNG too large (" + (bytes / 1024 / 1024)
				+ " MB), showing first frame only");
			return null;
		}

		byte[] extraBytes = extra.toByteArray();
		int[] canvas = new int[cw * ch];
		int[] backup = null;
		FrameInfo prev = null;
		NativeImage[] out = new NativeImage[infos.size()];
		int[] delays = new int[infos.size()];

		try {
			for (int i = 0; i < infos.size(); i++) {
				FrameInfo f = infos.get(i);
				if (prev != null) {
					if (prev.dispose == 1) {
						clear(canvas, cw, ch, prev);
					} else if (prev.dispose == 2 && backup != null) {
						canvas = backup;
					}
				}
				backup = f.dispose == 2 ? canvas.clone() : null;

				NativeImage img = decodeFrame(ihdr, extraBytes, f);
				try {
					draw(canvas, cw, ch, img, f);
				} finally {
					img.close();
				}

				NativeImage snap = new NativeImage(NativeImage.Format.RGBA, cw, ch, false);
				for (int y = 0; y < ch; y++) {
					for (int x = 0; x < cw; x++) {
						snap.setPixel(x, y, canvas[y * cw + x]);
					}
				}
				out[i] = snap;
				delays[i] = f.delayMs;
				prev = f;
			}
		} catch (IOException | RuntimeException e) {
			for (NativeImage n : out) {
				if (n != null) n.close();
			}
			throw e;
		}
		return new Result(cw, ch, out, delays);
	}

	private static NativeImage decodeFrame(byte[] ihdr, byte[] extra, FrameInfo f) throws IOException {
		byte[] hdr = ihdr.clone();
		writeInt(hdr, 0, f.w);
		writeInt(hdr, 4, f.h);
		ByteArrayOutputStream png = new ByteArrayOutputStream();
		png.write(SIG, 0, 8);
		writeChunk(png, "IHDR", hdr);
		png.write(extra, 0, extra.length);
		writeChunk(png, "IDAT", f.data.toByteArray());
		writeChunk(png, "IEND", new byte[0]);
		return NativeImage.read(new ByteArrayInputStream(png.toByteArray()));
	}

	private static void draw(int[] canvas, int cw, int ch, NativeImage img, FrameInfo f) {
		int w = Math.min(img.getWidth(), f.w);
		int h = Math.min(img.getHeight(), f.h);
		for (int y = 0; y < h; y++) {
			int cy = f.y + y;
			if (cy >= ch) break;
			for (int x = 0; x < w; x++) {
				int cx = f.x + x;
				if (cx >= cw) break;
				int src = img.getPixel(x, y);
				int idx = cy * cw + cx;
				canvas[idx] = f.blend == 0 ? src : over(src, canvas[idx]);
			}
		}
	}

	private static void clear(int[] canvas, int cw, int ch, FrameInfo f) {
		for (int y = f.y; y < Math.min(f.y + f.h, ch); y++) {
			for (int x = f.x; x < Math.min(f.x + f.w, cw); x++) {
				canvas[y * cw + x] = 0;
			}
		}
	}

	private static int over(int src, int dst) {
		int sa = src >>> 24;
		if (sa == 255) return src;
		if (sa == 0) return dst;
		float saF = sa / 255f;
		float daF = (dst >>> 24) / 255f;
		float oa = saF + daF * (1f - saF);
		if (oa <= 0f) return 0;
		int out = Math.round(oa * 255f) << 24;
		for (int shift = 0; shift <= 16; shift += 8) {
			float sc = (src >>> shift) & 255;
			float dc = (dst >>> shift) & 255;
			float oc = (sc * saF + dc * daF * (1f - saF)) / oa;
			out |= Math.min(255, Math.round(oc)) << shift;
		}
		return out;
	}

	private static int readInt(byte[] b, int o) {
		return ((b[o] & 255) << 24) | ((b[o + 1] & 255) << 16) | ((b[o + 2] & 255) << 8) | (b[o + 3] & 255);
	}

	private static void writeInt(byte[] b, int o, int v) {
		b[o] = (byte) (v >>> 24);
		b[o + 1] = (byte) (v >>> 16);
		b[o + 2] = (byte) (v >>> 8);
		b[o + 3] = (byte) v;
	}

	private static void writeChunk(ByteArrayOutputStream out, String type, byte[] body) {
		byte[] t = type.getBytes(StandardCharsets.US_ASCII);
		writeIntTo(out, body.length);
		out.write(t, 0, 4);
		out.write(body, 0, body.length);
		CRC32 crc = new CRC32();
		crc.update(t, 0, 4);
		crc.update(body, 0, body.length);
		writeIntTo(out, (int) crc.getValue());
	}

	private static void writeIntTo(ByteArrayOutputStream out, int v) {
		out.write(v >>> 24);
		out.write(v >>> 16);
		out.write(v >>> 8);
		out.write(v);
	}
                                     }
