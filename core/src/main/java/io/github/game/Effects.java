package io.github.game;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/** Partículas: estilhaços de vidro, faíscas, rastro de jato e flash das linhas. */
public class Effects {

    private static final float GRAVITY = -1800f;

    private static class Particle {
        float x, y, vx, vy;
        float rotation, spin;
        float width, height;
        float life, maxLife;
        float r, g, b, a;
        int texture;
        int kind;
    }

    private static final int SPARK = 0, FLASH = 1, TRAIL = 2, RING = 3, STREAK = 4;

    /** Diâmetro final da onda de choque: cobre o tabuleiro inteiro a partir de qualquer linha. */
    private static final float RING_MAX_SIZE = 2800f;

    private final List<Particle> shards = new ArrayList<>();
    private final List<Particle> glows = new ArrayList<>();
    private final Random random;

    private final Texture[] shardTextures;
    private final Texture glow;
    private final Texture ring;

    public Effects(Texture[] shardTextures, Texture glow, Texture ring, Random random) {
        this.shardTextures = shardTextures;
        this.glow = glow;
        this.ring = ring;
        this.random = random;
    }

    private float range(float min, float max) {
        return min + random.nextFloat() * (max - min);
    }

    /** Estilhaça um bloco de vidro em pedacinhos que voam e caem. */
    public void shatterBlock(float x, float y, float size, Color color, float explosionCenterX) {
        int pieces = 4;
        float sub = size / pieces;
        for (int i = 0; i < pieces; i++) {
            for (int j = 0; j < pieces; j++) {
                Particle p = new Particle();
                p.x = x + (i + 0.5f) * sub;
                p.y = y + (j + 0.5f) * sub;
                p.vx = (p.x - explosionCenterX) * range(0.8f, 2.2f) + range(-220f, 220f);
                p.vy = range(150f, 750f);
                p.rotation = range(0f, 360f);
                p.spin = range(-900f, 900f);
                p.width = p.height = range(sub * 0.7f, sub * 1.5f);
                p.maxLife = p.life = range(0.9f, 1.6f);
                p.r = color.r; p.g = color.g; p.b = color.b; p.a = 1f;
                p.texture = random.nextInt(shardTextures.length);
                shards.add(p);
            }
        }
        // Faíscas brancas
        for (int k = 0; k < 4; k++) {
            Particle s = new Particle();
            s.x = x + range(0f, size);
            s.y = y + range(0f, size);
            s.vx = range(-500f, 500f);
            s.vy = range(-200f, 600f);
            s.width = s.height = range(10f, 22f);
            s.maxLife = s.life = range(0.25f, 0.5f);
            s.r = s.g = s.b = 1f; s.a = 0.9f;
            s.kind = SPARK;
            glows.add(s);
        }
    }

    /** Flash horizontal sobre uma linha que acabou de ser destruída. */
    public void lineFlash(float x, float y, float width, float height) {
        Particle f = new Particle();
        f.x = x + width / 2f;
        f.y = y + height / 2f;
        f.width = width * 1.15f;
        f.height = height * 2.2f;
        f.maxLife = f.life = 0.35f;
        f.r = 0.85f; f.g = 0.95f; f.b = 1f; f.a = 0.9f;
        f.kind = FLASH;
        glows.add(f);
    }

    /** Onda de choque: dois anéis de luz que se expandem a partir do centro, o segundo um pouco atrasado. */
    public void shockwave(float x, float y, Color color) {
        for (int k = 0; k < 2; k++) {
            Particle w = new Particle();
            w.x = x;
            w.y = y;
            w.maxLife = 0.75f + 0.2f * k;
            // vida extra no segundo anel = atraso antes de aparecer
            w.life = w.maxLife + 0.12f * k;
            w.r = 0.6f + color.r * 0.4f;
            w.g = 0.6f + color.g * 0.4f;
            w.b = 0.6f + color.b * 0.4f;
            w.a = k == 0 ? 0.85f : 0.5f;
            w.kind = RING;
            glows.add(w);
        }
    }

    /** Faixa vertical de luz deixada por uma coluna da peça na queda rápida, de yBottom até yTop. */
    public void dropStreak(float x, float yBottom, float yTop, float cellSize, Color color) {
        Particle s = new Particle();
        s.x = x;
        s.y = (yBottom + yTop) / 2f;
        s.width = cellSize * 0.9f;
        s.height = (yTop - yBottom) * 1.1f;
        s.maxLife = s.life = 0.32f;
        s.r = 0.6f + color.r * 0.4f;
        s.g = 0.6f + color.g * 0.4f;
        s.b = 0.6f + color.b * 0.4f;
        s.a = 0.55f;
        s.kind = STREAK;
        glows.add(s);
    }

    /** Impacto da queda rápida: faíscas espirrando para os lados e um clarão rente ao chão. */
    public void impact(float x, float y, float width, Color color) {
        for (int k = 0; k < 6; k++) {
            Particle s = new Particle();
            float side = k % 2 == 0 ? -1f : 1f;
            s.x = x + side * range(0f, width * 0.5f);
            s.y = y + range(0f, 6f);
            s.vx = side * range(180f, 520f);
            s.vy = range(80f, 380f);
            s.width = s.height = range(8f, 18f);
            s.maxLife = s.life = range(0.2f, 0.45f);
            s.r = 0.7f + color.r * 0.3f;
            s.g = 0.7f + color.g * 0.3f;
            s.b = 0.7f + color.b * 0.3f;
            s.a = 0.9f;
            s.kind = SPARK;
            glows.add(s);
        }
        Particle f = new Particle();
        f.x = x;
        f.y = y;
        f.width = width * 1.6f;
        f.height = width * 0.7f;
        f.maxLife = f.life = 0.25f;
        f.r = 0.6f + color.r * 0.4f;
        f.g = 0.6f + color.g * 0.4f;
        f.b = 0.6f + color.b * 0.4f;
        f.a = 0.7f;
        f.kind = FLASH;
        glows.add(f);
    }

    /** Fumaça luminosa do rastro de jato deixado pela peça ao descer. */
    public void trail(float x, float y, float cellSize, Color color) {
        for (int k = 0; k < 3; k++) {
            Particle t = new Particle();
            t.x = x + range(-cellSize * 0.08f, cellSize * 0.08f);
            t.y = y + range(0f, cellSize);
            t.vx = range(-8f, 8f);
            t.vy = range(10f, 35f);
            t.width = range(cellSize * 0.45f, cellSize * 0.7f);
            t.height = t.width * 1.8f;
            t.maxLife = t.life = range(0.35f, 0.65f);
            t.r = 0.55f + color.r * 0.45f;
            t.g = 0.55f + color.g * 0.45f;
            t.b = 0.55f + color.b * 0.45f;
            t.a = 0.22f;
            t.kind = TRAIL;
            glows.add(t);
        }
    }

    public void update(float delta) {
        Iterator<Particle> it = shards.iterator();
        while (it.hasNext()) {
            Particle p = it.next();
            p.life -= delta;
            if (p.life <= 0) { it.remove(); continue; }
            p.vy += GRAVITY * delta;
            p.x += p.vx * delta;
            p.y += p.vy * delta;
            p.rotation += p.spin * delta;
        }
        it = glows.iterator();
        while (it.hasNext()) {
            Particle p = it.next();
            p.life -= delta;
            if (p.life <= 0) { it.remove(); continue; }
            if (p.kind == SPARK) p.vy += GRAVITY * 0.4f * delta;
            p.x += p.vx * delta;
            p.y += p.vy * delta;
            if (p.kind == FLASH) {
                // flash da linha: estreita verticalmente ao sumir
                p.height *= 1f - delta * 3f;
            } else if (p.kind == TRAIL) {
                // rastro: se espalha conforme se dissipa
                p.width *= 1f + delta * 0.6f;
            } else if (p.kind == STREAK) {
                // faixa da queda: afina enquanto some
                p.width *= 1f - delta * 4f;
            } else if (p.kind == RING) {
                // anel: cresce rápido no início e desacelera (ease-out)
                float t = Math.max(0f, Math.min(1f, 1f - p.life / p.maxLife));
                p.width = p.height = RING_MAX_SIZE * (1f - (1f - t) * (1f - t));
            }
        }
    }

    /** Rastro e faixas da queda rápida (atrás da peça). Deve ser chamado entre batch.begin()/end(). */
    public void drawTrail(SpriteBatch batch) {
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
        for (Particle p : glows) {
            if (p.kind != TRAIL && p.kind != STREAK) continue;
            drawGlow(batch, p);
        }
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
    }

    /** Estilhaços, faíscas e flashes (na frente das peças). */
    public void drawFront(SpriteBatch batch) {
        for (Particle p : shards) {
            float t = p.life / p.maxLife;
            float alpha = t < 0.5f ? t / 0.5f : 1f;
            batch.setColor(p.r, p.g, p.b, alpha * 0.85f);
            drawRotated(batch, shardTextures[p.texture], p, p.width);
        }
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
        for (Particle p : shards) {
            // reflexo cintilante do vidro conforme o pedaço gira
            float t = p.life / p.maxLife;
            float glint = (float) Math.pow(Math.abs(Math.sin(Math.toRadians(p.rotation))), 6);
            batch.setColor(1f, 1f, 1f, glint * 0.7f * Math.min(1f, t * 2f));
            drawRotated(batch, shardTextures[p.texture], p, p.width);
        }
        for (Particle p : glows) {
            if (p.kind == TRAIL || p.kind == STREAK) continue; // já desenhados atrás da peça
            drawGlow(batch, p);
        }
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        batch.setColor(Color.WHITE);
    }

    private void drawGlow(SpriteBatch batch, Particle p) {
        float t = Math.min(1f, p.life / p.maxLife);
        if (p.kind == RING) {
            if (p.life > p.maxLife) return; // ainda no atraso
            batch.setColor(p.r, p.g, p.b, p.a * t * t);
            batch.draw(ring, p.x - p.width / 2f, p.y - p.height / 2f, p.width, p.height);
            return;
        }
        batch.setColor(p.r, p.g, p.b, p.a * t);
        batch.draw(glow, p.x - p.width / 2f, p.y - p.height / 2f, p.width, p.height);
    }

    private void drawRotated(SpriteBatch batch, Texture tex, Particle p, float size) {
        float half = size / 2f;
        batch.draw(tex, p.x - half, p.y - half, half, half, size, size, 1f, 1f, p.rotation,
            0, 0, tex.getWidth(), tex.getHeight(), false, false);
    }

    public void clear() {
        shards.clear();
        glows.clear();
    }
}
