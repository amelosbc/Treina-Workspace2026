/**
 * @file generate-icons.js
 * @description Generates placeholder PNG icons for the extension using canvas.
 * Run with: node generate-icons.js
 * Requires: npm install canvas
 */

const { createCanvas } = require("canvas");
const fs = require("fs");
const path = require("path");

const SIZES = [16, 32, 48, 128];
const OUT_DIR = path.join(__dirname, "icons");

if (!fs.existsSync(OUT_DIR)) fs.mkdirSync(OUT_DIR);

SIZES.forEach((size) => {
  const canvas = createCanvas(size, size);
  const ctx = canvas.getContext("2d");

  // Background
  ctx.fillStyle = "#0e0e10";
  ctx.beginPath();
  ctx.roundRect(0, 0, size, size, size * 0.15);
  ctx.fill();

  // Accent circle
  ctx.fillStyle = "#f0522a";
  const r = size * 0.28;
  ctx.beginPath();
  ctx.arc(size / 2, size / 2, r, 0, Math.PI * 2);
  ctx.fill();

  // Inner dot
  ctx.fillStyle = "#0e0e10";
  const ri = size * 0.12;
  ctx.beginPath();
  ctx.arc(size / 2, size / 2, ri, 0, Math.PI * 2);
  ctx.fill();

  const outPath = path.join(OUT_DIR, `icon${size}.png`);
  fs.writeFileSync(outPath, canvas.toBuffer("image/png"));
  console.log(`Generated: ${outPath}`);
});
