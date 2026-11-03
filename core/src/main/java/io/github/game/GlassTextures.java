package io.github.game;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;

import java.util.Random;

/** Gera em código todas as texturas do jogo (dispensa arquivos .png). */
public final class GlassTextures {

    private GlassTextures() { }

    private static Texture toTexture(Pixmap pixmap) {
        Texture texture = new Texture(pixmap);
        texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        pixmap.dispose();
        return texture;
    }

    private static int rgba(float r, float g, float b, float a) {
        return Color.rgba8888(clamp(r), clamp(g), clamp(b), clamp(a));
    }

    private static float clamp(float v) {
        return v < 0f ? 0f : (v > 1f ? 1f : v);
    }

    /** Pixel branco 1x1, usado para desenhar retângulos e linhas. */
    public static Texture createPixel() {
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.WHITE);
        pixmap.fill();
        return toTexture(pixmap);
    }

    /**
     * Corpo do bloco de vidro: branco/cinza (a cor da peça é aplicada via batch.setColor),
     * com bisel claro em cima/esquerda e escuro em baixo/direita para o efeito 3D,
     * e centro semitransparente.
     */
    public static Texture createBlockBody(int size) {
        Pixmap pixmap = new Pixmap(size, size, Pixmap.Format.RGBA8888);
        pixmap.setBlending(Pixmap.Blending.None);
        float bevel = size * 0.14f;
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                float dl = x, dr = size - 1 - x, dt = y, db = size - 1 - y;
                float edge = Math.min(Math.min(dl, dr), Math.min(dt, db));
                float bright;
                float alpha;
                if (edge < 1.5f) {
                    // Contorno externo escuro
                    bright = 0.35f;
                    alpha = 1f;
                } else if (edge < bevel) {
                    // Bisel: a face mais próxima define a iluminação
                    if (edge == dt) bright = 1.00f;
                    else if (edge == dl) bright = 0.90f;
                    else if (edge == dr) bright = 0.58f;
                    else bright = 0.45f;
                    alpha = 0.95f;
                } else if (edge < bevel + 1.5f) {
                    // Filete brilhante na borda interna do bisel
                    bright = 1f;
                    alpha = 0.9f;
                } else {
                    // Centro de vidro: gradiente vertical e mais transparente
                    float t = y / (float) size;
                    bright = 0.98f - 0.30f * t;
                    alpha = 0.55f + 0.20f * t;
                }
                pixmap.drawPixel(x, y, rgba(bright, bright, bright, alpha));
            }
        }
        return toTexture(pixmap);
    }

    /** Reflexo branco (desenhado sem tinta) que dá o aspecto de vidro polido. */
    public static Texture createBlockGloss(int size) {
        Pixmap pixmap = new Pixmap(size, size, Pixmap.Format.RGBA8888);
        pixmap.setBlending(Pixmap.Blending.None);
        float bevel = size * 0.14f;
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                float u = x / (float) size, v = y / (float) size;
                float a = 0f;

                // Brilho elíptico no canto superior esquerdo
                float ex = (u - 0.32f) / 0.42f, ey = (v - 0.25f) / 0.22f;
                float e = ex * ex + ey * ey;
                if (e < 1f) a += 0.38f * (1f - e);

                // Faixa diagonal de reflexo
                float d = Math.abs((u + v) - 0.95f);
                if (d < 0.07f) a += 0.22f * (1f - d / 0.07f);

                // Linha de luz no topo do bisel
                if (y >= 2 && y < 4 && x > bevel && x < size - bevel) a += 0.55f;

                // Ponto especular
                float sx = u - 0.2f, sy = v - 0.2f;
                float s = (sx * sx + sy * sy) / 0.004f;
                if (s < 1f) a += 0.6f * (1f - s);

                pixmap.drawPixel(x, y, rgba(1f, 1f, 1f, Math.min(a, 0.85f)));
            }
        }
        return toTexture(pixmap);
    }

    /** Gradiente radial branco (brilho suave para rastro, faíscas e orbes do fundo). */
    public static Texture createGlow(int size) {
        Pixmap pixmap = new Pixmap(size, size, Pixmap.Format.RGBA8888);
        pixmap.setBlending(Pixmap.Blending.None);
        float c = (size - 1) / 2f;
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                float dx = (x - c) / c, dy = (y - c) / c;
                float d = (float) Math.sqrt(dx * dx + dy * dy);
                float a = d >= 1f ? 0f : (1f - d) * (1f - d);
                pixmap.drawPixel(x, y, rgba(1f, 1f, 1f, a));
            }
        }
        return toTexture(pixmap);
    }

    /** Anel branco de borda suave (onda de choque); a borda externa é mais nítida que a interna. */
    public static Texture createRing(int size) {
        Pixmap pixmap = new Pixmap(size, size, Pixmap.Format.RGBA8888);
        pixmap.setBlending(Pixmap.Blending.None);
        float c = (size - 1) / 2f;
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                float dx = (x - c) / c, dy = (y - c) / c;
                float d = (float) Math.sqrt(dx * dx + dy * dy);
                float a;
                if (d >= 1f) a = 0f;
                else if (d > 0.9f) a = (1f - d) / 0.1f;
                else a = Math.max(0f, (d - 0.55f) / 0.35f);
                pixmap.drawPixel(x, y, rgba(1f, 1f, 1f, a * a));
            }
        }
        return toTexture(pixmap);
    }

    /** Bloco de pedra: rocha manchada marrom-acinzentada, bisel irregular e rachaduras escuras. */
    public static Texture createStone(int size, Random random) {
        Pixmap pixmap = new Pixmap(size, size, Pixmap.Format.RGBA8888);
        pixmap.setBlending(Pixmap.Blending.None);

        // Manchas: grade grossa de valores aleatórios, interpolada
        int cells = 6;
        float[][] blotches = new float[cells + 1][cells + 1];
        for (float[] row : blotches) {
            for (int i = 0; i < row.length; i++) row[i] = random.nextFloat();
        }
        float bevel = size * 0.14f;
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                float gx = x * cells / (float) size, gy = y * cells / (float) size;
                int ix = (int) gx, iy = (int) gy;
                float fx = gx - ix, fy = gy - iy;
                float blotch = (blotches[iy][ix] * (1 - fx) + blotches[iy][ix + 1] * fx) * (1 - fy)
                    + (blotches[iy + 1][ix] * (1 - fx) + blotches[iy + 1][ix + 1] * fx) * fy;

                float light = 0.72f + 0.42f * blotch + (random.nextFloat() - 0.5f) * 0.14f;
                float dl = x, dr = size - 1 - x, dt = y, db = size - 1 - y;
                float edge = Math.min(Math.min(dl, dr), Math.min(dt, db));
                if (edge < 1.5f) light = 0.25f;
                else if (edge < bevel) light *= (edge == dt || edge == dl) ? 1.3f : 0.6f;

                pixmap.drawPixel(x, y, rgba(0.56f * light, 0.46f * light, 0.36f * light, 1f));
            }
        }

        // Rachaduras em zigue-zague partindo do centro
        pixmap.setColor(0.13f, 0.10f, 0.08f, 1f);
        for (int s = 0; s < 4; s++) {
            double ang = Math.PI / 2 * s + random.nextFloat();
            float px = size * 0.5f, py = size * 0.5f;
            for (int seg = 0; seg < 4; seg++) {
                ang += (random.nextFloat() - 0.5f) * 1.2f;
                float len = size * 0.11f;
                float qx = px + (float) Math.cos(ang) * len, qy = py + (float) Math.sin(ang) * len;
                pixmap.drawLine(Math.round(px), Math.round(py), Math.round(qx), Math.round(qy));
                px = qx;
                py = qy;
            }
        }
        return toTexture(pixmap);
    }

    /** Fragmentos de vidro: polígonos irregulares brancos com arestas brilhantes. */
    public static Texture[] createShards(int count, int size, Random random) {
        Texture[] shards = new Texture[count];
        for (int i = 0; i < count; i++) {
            Pixmap pixmap = new Pixmap(size, size, Pixmap.Format.RGBA8888);
            pixmap.setColor(0f, 0f, 0f, 0f);
            pixmap.fill();
            int vertices = 3 + random.nextInt(2);
            int[] px = new int[vertices];
            int[] py = new int[vertices];
            float c = size / 2f;
            for (int v = 0; v < vertices; v++) {
                double ang = (Math.PI * 2 * v) / vertices + random.nextFloat() * 0.9f;
                float rad = c * (0.55f + random.nextFloat() * 0.42f);
                px[v] = Math.round(c + (float) Math.cos(ang) * rad);
                py[v] = Math.round(c + (float) Math.sin(ang) * rad);
            }
            pixmap.setColor(1f, 1f, 1f, 0.75f);
            for (int v = 1; v < vertices - 1; v++) {
                pixmap.fillTriangle(px[0], py[0], px[v], py[v], px[v + 1], py[v + 1]);
            }
            pixmap.setColor(1f, 1f, 1f, 1f);
            for (int v = 0; v < vertices; v++) {
                int n = (v + 1) % vertices;
                pixmap.drawLine(px[v], py[v], px[n], py[n]);
            }
            shards[i] = toTexture(pixmap);
        }
        return shards;
    }

    /** Rachaduras em teia (vidro temperado) exibidas um instante antes de estilhaçar. */
    public static Texture[] createCracks(int count, int size, Random random) {
        Texture[] cracks = new Texture[count];
        for (int i = 0; i < count; i++) {
            Pixmap pixmap = new Pixmap(size, size, Pixmap.Format.RGBA8888);
            pixmap.setColor(0f, 0f, 0f, 0f);
            pixmap.fill();
            int cx = size / 3 + random.nextInt(size / 3);
            int cy = size / 3 + random.nextInt(size / 3);
            int spokes = 7 + random.nextInt(4);
            int[] ex = new int[spokes];
            int[] ey = new int[spokes];
            pixmap.setColor(1f, 1f, 1f, 0.95f);
            for (int s = 0; s < spokes; s++) {
                double ang = (Math.PI * 2 * s) / spokes + random.nextFloat() * 0.4f;
                float len = size * (0.6f + random.nextFloat() * 0.4f);
                ex[s] = Math.round(cx + (float) Math.cos(ang) * len);
                ey[s] = Math.round(cy + (float) Math.sin(ang) * len);
                pixmap.drawLine(cx, cy, ex[s], ey[s]);
            }
            // Anéis concêntricos ligando os raios
            pixmap.setColor(1f, 1f, 1f, 0.6f);
            for (float ring = 0.3f; ring < 0.9f; ring += 0.28f) {
                for (int s = 0; s < spokes; s++) {
                    int n = (s + 1) % spokes;
                    int ax = Math.round(cx + (ex[s] - cx) * ring), ay = Math.round(cy + (ey[s] - cy) * ring);
                    int bx = Math.round(cx + (ex[n] - cx) * ring), by = Math.round(cy + (ey[n] - cy) * ring);
                    pixmap.drawLine(ax, ay, bx, by);
                }
            }
            cracks[i] = toTexture(pixmap);
        }
        return cracks;
    }

    /** Seta branca apontando para cima (rotacionada no desenho para as outras direções). */
    public static Texture createArrow(int size) {
        Pixmap pixmap = new Pixmap(size, size, Pixmap.Format.RGBA8888);
        pixmap.setColor(0f, 0f, 0f, 0f);
        pixmap.fill();
        pixmap.setColor(Color.WHITE);
        int m = size / 6;
        // Pixmap tem y para baixo: ponta em cima
        pixmap.fillTriangle(size / 2, m, m, size / 2 + m / 2, size - m, size / 2 + m / 2);
        pixmap.fillRectangle(size / 2 - size / 8, size / 2, size / 4, size / 2 - m);
        return toTexture(pixmap);
    }

    /** Plano de fundo futurista: gradiente, nebulosas suaves, estrelas e grade em perspectiva. */
    public static Texture createBackground(int width, int height, Random random) {
        Pixmap pixmap = new Pixmap(width, height, Pixmap.Format.RGBA8888);
        pixmap.setBlending(Pixmap.Blending.None);

        // Nebulosas: (x, y, raio, r, g, b) em coordenadas normalizadas (y para baixo)
        float[][] nebulas = {
            {0.15f, 0.20f, 0.45f, 0.10f, 0.35f, 0.60f},
            {0.85f, 0.35f, 0.50f, 0.45f, 0.10f, 0.55f},
            {0.50f, 0.65f, 0.40f, 0.05f, 0.40f, 0.50f},
        };
        float horizon = 0.70f;

        for (int y = 0; y < height; y++) {
            float v = y / (float) height;
            for (int x = 0; x < width; x++) {
                float u = x / (float) width;
                float r = 0.015f + 0.05f * v;
                float g = 0.020f + 0.02f * v;
                float b = 0.070f + 0.07f * v;
                for (float[] n : nebulas) {
                    float dx = (u - n[0]) / n[2], dy = (v - n[1]) / n[2];
                    float k = (float) Math.exp(-(dx * dx + dy * dy) * 2.2f) * 0.22f;
                    r += n[3] * k;
                    g += n[4] * k;
                    b += n[5] * k;
                }
                // Brilho do horizonte
                float hz = Math.abs(v - horizon);
                float hk = (float) Math.exp(-hz * hz * 900f) * 0.35f;
                r += 0.8f * hk;
                g += 0.2f * hk;
                b += 0.9f * hk;
                pixmap.drawPixel(x, y, rgba(r, g, b, 1f));
            }
        }

        pixmap.setBlending(Pixmap.Blending.SourceOver);

        // Estrelas na parte de cima
        for (int i = 0; i < 260; i++) {
            int sx = random.nextInt(width);
            int sy = random.nextInt((int) (height * horizon));
            float a = 0.25f + random.nextFloat() * 0.6f;
            pixmap.setColor(0.8f, 0.9f, 1f, a);
            if (random.nextFloat() < 0.12f) pixmap.fillCircle(sx, sy, 2);
            else pixmap.drawPixel(sx, sy);
        }

        // Grade em perspectiva abaixo do horizonte
        int hy = (int) (height * horizon);
        int depth = height - hy;
        for (int i = 1; i <= 14; i++) {
            float t = i / 14f;
            int ly = hy + Math.round(depth * t * t);
            pixmap.setColor(0.25f, 0.75f, 1f, 0.08f + 0.22f * t);
            pixmap.drawLine(0, ly, width, ly);
        }
        int vanishX = width / 2;
        for (int i = -16; i <= 16; i++) {
            int bx = vanishX + i * (width / 7);
            pixmap.setColor(0.25f, 0.75f, 1f, 0.16f);
            pixmap.drawLine(vanishX + i * 6, hy, bx, height);
        }
        return toTexture(pixmap);
    }
}
