// Validates a wasm module without instantiating it.
import fs from 'node:fs';

const file = process.argv[2];
const bytes = fs.readFileSync(file);
for (const opts of [{}, { builtins: ['js-string'] }]) {
  try {
    new WebAssembly.Module(bytes, opts);
    console.log('OK', JSON.stringify(opts));
    process.exit(0);
  } catch (e) {
    console.log('FAIL', JSON.stringify(opts), e.message);
  }
}
process.exit(1);
