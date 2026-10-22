#!/usr/bin/env node
/**
 * freehands trinket item textures — dependency-free 16x16 pixel-art generator.
 *
 * Colours are sampled from the vanilla textures shipped in the 1.20.1 client jar
 * (item/iron_ingot, item/diamond, item/netherite_ingot), so the trinkets read as
 * native Minecraft materials. Nothing here goes through an image model: every
 * pixel is placed by hand in this file and written straight at final 16x16 size.
 *
 * Usage:
 *   node tools/art/trinkets.mjs            # write variant K (default) into the item texture folder
 *   node tools/art/trinkets.mjs K          # write variant K
 *   node tools/art/trinkets.mjs I          # write variant I
 *   node tools/art/trinkets.mjs L --dry    # print ASCII + palette stats only
 *
 * Variants:
 *   K  medallion — round disc, diamond setting, material gem core                      (default)
 *   I  placard   — squared plate with a diamond setting
 *   L  pendant   — bail ring on top (rejected: reads as a padlock, not a trinket)
 */
import zlib from 'node:zlib';
import fs from 'node:fs';
import path from 'node:path';

// ---------------------------------------------------------------- vanilla ramps
// dark -> bright, sampled from the vanilla 16x16 item textures
const RAMP = {
  iron:      ['#353535', '#5e5e5e', '#727272', '#828282', '#a8a8a8', '#d8d8d8', '#ffffff'],
  diamond:   ['#145e53', '#11727a', '#1c919a', '#1aaaa7', '#20c5b5', '#4aedd9', '#d5fff6'],
  netherite: ['#111111', '#271c1d', '#31292a', '#4c4143', '#4d494d', '#5a575a', '#737173'],
};
const ACCENT = {
  iron:      {},
  diamond:   {},
  netherite: { wine: '#7c1f38', wineL: '#b4536a', gold: '#c9a227' },
};

// ---------------------------------------------------------------- png output
const CRC = (() => {
  const t = new Int32Array(256);
  for (let n = 0; n < 256; n++) { let c = n; for (let k = 0; k < 8; k++) c = (c & 1) ? (0xedb88320 ^ (c >>> 1)) : (c >>> 1); t[n] = c; }
  return t;
})();
function crc32(b) { let c = -1; for (let i = 0; i < b.length; i++) c = CRC[(c ^ b[i]) & 0xff] ^ (c >>> 8); return (c ^ -1) >>> 0; }
function chunk(type, data) {
  const len = Buffer.alloc(4); len.writeUInt32BE(data.length, 0);
  const body = Buffer.concat([Buffer.from(type, 'ascii'), data]);
  const crc = Buffer.alloc(4); crc.writeUInt32BE(crc32(body), 0);
  return Buffer.concat([len, body, crc]);
}
function writePng(file, size, rgba) {
  const ihdr = Buffer.alloc(13);
  ihdr.writeUInt32BE(size, 0); ihdr.writeUInt32BE(size, 4); ihdr[8] = 8; ihdr[9] = 6;
  const raw = Buffer.alloc((size * 4 + 1) * size);
  for (let y = 0; y < size; y++) Buffer.from(rgba.buffer, rgba.byteOffset + y * size * 4, size * 4).copy(raw, y * (size * 4 + 1) + 1);
  fs.writeFileSync(file, Buffer.concat([
    Buffer.from([0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a]),
    chunk('IHDR', ihdr), chunk('IDAT', zlib.deflateSync(raw, { level: 9 })), chunk('IEND', Buffer.alloc(0)),
  ]));
}

// ---------------------------------------------------------------- helpers
const DIST = (x, y, cx, cy) => Math.hypot(x - cx, y - cy);
// directional light from the upper left, same convention the vanilla textures use
const SHADE = (x, y, cx, cy) => {
  const dx = x - cx, dy = y - cy, l = Math.hypot(dx, dy) || 1;
  return Math.max(0, Math.min(1, ((-(dx / l) * 0.62 - (dy / l) * 0.78) + 1) / 2));
};
const pick = (kind, s) => {
  const r = RAMP[kind];
  return r[Math.max(0, Math.min(r.length - 1, Math.round(s * (r.length - 1))))];
};
const empty = () => Array.from({ length: 16 }, () => Array(16).fill(null));

function spans(rows) {
  const inBody = (x, y) => {
    const s = rows[y];
    return s ? (x >= s[0] && x <= s[1]) : false;
  };
  return inBody;
}

// ---------------------------------------------------------------- material cores
// A 6x6 cut-gem diamond sits in the setting; each tier gets its own two- or
// three-step material ramp so the core still reads as that material.
const CORE_RAMP = {
  iron:      ['#a8a8a8', '#d8d8d8', '#ffffff'],
  diamond:   ['#1c919a', '#20c5b5', '#4aedd9', '#d5fff6'],
  netherite: ['#6b1a30', '#7c1f38', '#a8394f', '#c06a80'],
};
function core(kind) {
  const ramp = CORE_RAMP[kind];
  const cells = [];
  for (let y = 5; y <= 10; y++) for (let x = 5; x <= 10; x++) {
    if (Math.abs(x - 7.5) + Math.abs(y - 7.5) > 2.6) continue;
    const s = SHADE(x, y, 7.5, 7.5);
    cells.push([x, y, ramp[Math.max(0, Math.min(ramp.length - 1, Math.round(s * (ramp.length - 1))))]]);
  }
  return cells;
}
function rivets(kind, pts) {
  const a = ACCENT[kind];
  return kind === 'netherite' ? pts.map(([x, y]) => [x, y, a.gold]) : [];
}

function paint(g, cols) {
  for (const [x, y, c] of cols) if (g[y] && g[y][x]) g[y][x] = c;
  return g;
}

// ---------------------------------------------------------------- variant I: placard
const I_SPANS = { 2: [5, 10], 3: [3, 12], 4: [2, 13], 5: [2, 13], 6: [2, 13], 7: [2, 13], 8: [2, 13], 9: [2, 13], 10: [2, 13], 11: [2, 13], 12: [3, 12], 13: [5, 10] };
function buildI(kind) {
  const inBody = spans(I_SPANS);
  const g = empty();
  for (let y = 2; y <= 13; y++) for (let x = 2; x <= 13; x++) {
    if (!inBody(x, y)) continue;
    const s = SHADE(x, y, 7.5, 7.5);
    const near = !inBody(x - 1, y) || !inBody(x + 1, y) || !inBody(x, y - 1) || !inBody(x, y + 1);
    g[y][x] = near ? pick(kind, 0.55 + 0.45 * s) : pick(kind, 0.35 + 0.35 * s);
  }
  // squared recess with a diamond setting
  for (let y = 4; y <= 11; y++) for (let x = 4; x <= 11; x++) {
    if (!g[y][x]) continue;
    const d = Math.abs(x - 7.5) + Math.abs(y - 7.5);
    g[y][x] = d > 4.5 ? pick(kind, 0.12) : d > 3.5 ? pick(kind, 0.0) : d > 2.5 ? pick(kind, 0.05) : pick(kind, 0.0);
  }
  paint(g, core(kind));
  paint(g, rivets(kind, [[3, 5], [12, 5], [3, 10], [12, 10]]));
  return g;
}

// ---------------------------------------------------------------- variant K: medallion
const K_SPANS = { 2: [6, 9], 3: [4, 11], 4: [3, 12], 5: [2, 13], 6: [2, 13], 7: [2, 13], 8: [2, 13], 9: [2, 13], 10: [2, 13], 11: [3, 12], 12: [4, 11], 13: [6, 9] };
function buildK(kind) {
  const inBody = spans(K_SPANS);
  const g = empty();
  for (let y = 2; y <= 13; y++) for (let x = 2; x <= 13; x++) {
    if (!inBody(x, y)) continue;
    const s = SHADE(x, y, 7.5, 7.5);
    const d = Math.abs(x - 7.5) + Math.abs(y - 7.5);
    const near = !inBody(x - 1, y) || !inBody(x + 1, y) || !inBody(x, y - 1) || !inBody(x, y + 1);
    const near2 = !inBody(x - 2, y) || !inBody(x + 2, y) || !inBody(x, y - 2) || !inBody(x, y + 2);
    let col;
    if (near) col = pick(kind, 0.50 + 0.50 * s);
    else if (near2) col = pick(kind, 0.32 + 0.42 * s);
    else if (d > 3.4) col = pick(kind, 0.18 + 0.30 * s);
    else if (d > 2.6) col = pick(kind, 0.05 + 0.10 * s);
    else col = pick(kind, 0.0);
    g[y][x] = col;
  }
  for (let y = 3; y <= 12; y++) for (let x = 3; x <= 12; x++) {
    if (!g[y][x]) continue;
    const d = Math.abs(x - 7.5) + Math.abs(y - 7.5);
    if (d > 3.4 && d <= 4.5) g[y][x] = pick(kind, 0.02);
  }
  paint(g, core(kind));
  paint(g, rivets(kind, [[3, 5], [12, 5], [3, 10], [12, 10]]));
  return g;
}

// ---------------------------------------------------------------- variant L: pendant
const L_SPANS = { 4: [6, 9], 5: [3, 12], 6: [2, 13], 7: [2, 13], 8: [2, 13], 9: [2, 13], 10: [2, 13], 11: [2, 13], 12: [3, 12], 13: [4, 11], 14: [6, 9] };
function buildL(kind) {
  const inBody = spans(L_SPANS);
  const g = empty();
  // bail ring: top bar, two sides with an open 2x1 hole, bottom bar
  for (let x = 6; x <= 9; x++) g[1][x] = pick(kind, 0.85);
  g[2][6] = pick(kind, 0.55);
  g[2][9] = pick(kind, 0.35);
  for (let x = 6; x <= 9; x++) g[3][x] = pick(kind, 0.45);
  for (let y = 4; y <= 14; y++) for (let x = 2; x <= 13; x++) {
    if (!inBody(x, y)) continue;
    const s = SHADE(x, y, 7.5, 9);
    const d = Math.abs(x - 7.5) + Math.abs(y - 9.5);
    const near = !inBody(x - 1, y) || !inBody(x + 1, y) || !inBody(x, y - 1) || !inBody(x, y + 1);
    const near2 = !inBody(x - 2, y) || !inBody(x + 2, y) || !inBody(x, y - 2) || !inBody(x, y + 2);
    let col;
    if (near) col = pick(kind, 0.48 + 0.50 * s);
    else if (near2) col = pick(kind, 0.30 + 0.40 * s);
    else if (d > 3.4) col = pick(kind, 0.16 + 0.28 * s);
    else if (d > 2.4) col = pick(kind, 0.04 + 0.10 * s);
    else col = pick(kind, 0.0);
    g[y][x] = col;
  }
  for (let y = 7; y <= 12; y++) for (let x = 4; x <= 11; x++) {
    if (!g[y][x]) continue;
    const d = Math.abs(x - 7.5) + Math.abs(y - 9.5);
    if (d > 3.4 && d <= 4.4) g[y][x] = pick(kind, 0.02);
  }
  paint(g, core(kind).map(([x, y, c]) => [x, y + 2, c]));
  paint(g, rivets(kind, [[3, 8], [12, 8], [3, 11], [12, 11]]));
  return g;
}

const BUILDERS = { I: buildI, K: buildK, L: buildL };

// ---------------------------------------------------------------- output
function toRgba(g) {
  const rgba = new Uint8Array(16 * 16 * 4);
  for (let y = 0; y < 16; y++) for (let x = 0; x < 16; x++) {
    const c = g[y][x], i = (y * 16 + x) * 4;
    if (!c) { rgba[i + 3] = 0; continue; }
    const s = c.replace('#', '');
    rgba[i] = parseInt(s.slice(0, 2), 16);
    rgba[i + 1] = parseInt(s.slice(2, 4), 16);
    rgba[i + 2] = parseInt(s.slice(4, 6), 16);
    rgba[i + 3] = 255;
  }
  return rgba;
}

function report(name, rgba) {
  const colors = new Set();
  let opaque = 0, edge = false;
  for (let y = 0; y < 16; y++) for (let x = 0; x < 16; x++) {
    const i = (y * 16 + x) * 4;
    if (rgba[i + 3] === 0) continue;
    opaque++;
    colors.add(rgba[i] + ',' + rgba[i + 1] + ',' + rgba[i + 2]);
  }
  for (let x = 0; x < 16; x++) if (rgba[x * 4 + 3] || rgba[(15 * 16 + x) * 4 + 3]) edge = true;
  for (let y = 0; y < 16; y++) if (rgba[y * 16 * 4 + 3] || rgba[(y * 16 + 15) * 4 + 3]) edge = true;
  const ok = colors.size <= 16 && !edge && opaque > 0;
  console.log('  ' + name.padEnd(18) + ' colors=' + colors.size + ' opaque=' + opaque + ' edgeTouch=' + edge + ' ' + (ok ? 'OK' : 'FAIL'));
  return ok;
}

const args = process.argv.slice(2);
const variant = (args.find(a => /^[IKL]$/i.test(a)) || 'K').toUpperCase();
const dry = args.includes('--dry');
const outDir = (() => {
  const i = args.indexOf('--out');
  if (i >= 0 && args[i + 1]) return args[i + 1];
  return path.resolve(process.cwd(), 'src/main/resources/assets/freehands/textures/item');
})();

const build = BUILDERS[variant];
if (!build) { console.error('unknown variant ' + variant); process.exit(2); }
fs.mkdirSync(outDir, { recursive: true });
console.log('variant ' + variant + ' -> ' + outDir);
let allOk = true;
for (const kind of ['iron', 'diamond', 'netherite']) {
  const g = build(kind);
  const rgba = toRgba(g);
  allOk = report(kind + '_trinket.png', rgba) && allOk;
  if (dry) {
    for (const row of g) console.log('  ' + row.map(c => (c ? '#' : '.')).join(''));
  } else {
    writePng(path.join(outDir, kind + '_trinket.png'), 16, rgba);
  }
}
process.exit(allOk ? 0 : 1);