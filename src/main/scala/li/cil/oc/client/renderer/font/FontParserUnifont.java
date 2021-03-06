package li.cil.oc.client.renderer.font;

import li.cil.oc.OpenComputers;
import org.lwjgl.BufferUtils;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;

public class FontParserUnifont implements IGlyphProvider {
    private final byte[][] glyphs;

    public FontParserUnifont() throws Exception {
        OpenComputers.log().info("Initialized Unifont glyph provider.");
        glyphs = new byte[65536][];
        final InputStream font = getClass().getResourceAsStream("/assets/opencomputers/unifont.hex");
        final BufferedReader input = new BufferedReader(new InputStreamReader(font));
        String line;
        int glyphCount = 0;
        while ((line = input.readLine()) != null) {
            glyphCount++;
            final String[] info = line.split(":");
            final int charCode = Integer.parseInt(info[0], 16);
            final byte[] glyph = new byte[info[1].length() >> 1];
            for (int i = 0; i < glyph.length; i++) {
                glyph[i] = (byte) Integer.parseInt(info[1].substring(i * 2, i * 2 + 2), 16);
            }
            glyphs[charCode] = glyph;
        }
        OpenComputers.log().info("Loaded " + glyphCount + " glyphs.");
    }

    private static final byte[] b_set = {(byte) 255, (byte) 255, (byte) 255, (byte) 255};
    private static final byte[] b_unset = {0, 0, 0, 0};

    @Override
    public ByteBuffer getGlyph(int charCode) {
        if (charCode >= 65536 || glyphs[charCode] == null || glyphs[charCode].length == 0)
            return null;
        final byte[] glyph = glyphs[charCode];
        int glyphWidth;
        if (glyph.length == 16) glyphWidth = 8;
        else if (glyph.length == 32) glyphWidth = 16;
        else return null;
        final ByteBuffer buffer = BufferUtils.createByteBuffer(glyphWidth * 16 * 4);
        for (byte aGlyph : glyph) {
            int c = ((int) aGlyph) & 0xFF;
            for (int j = 0; j < 8; j++) {
                if ((c & 128) > 0) buffer.put(b_set);
                else buffer.put(b_unset);
                c <<= 1;
            }
        }
        buffer.rewind();
        return buffer;
    }

    @Override
    public int getGlyphWidth() {
        return 8;
    }

    @Override
    public int getGlyphHeight() {
        return 16;
    }
}
