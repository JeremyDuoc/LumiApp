const {test}=require("node:test");
const assert=require("node:assert/strict");
const {clean,docId,CATEGORIES,STATUSES}=require("./voice")._voice;

test("solo categorías y estados autorizados",()=>{
  assert.ok(CATEGORIES.includes("Registro diario"));
  assert.ok(STATUSES.includes("En desarrollo"));
  assert.ok(!STATUSES.includes("Administrador"));
});
test("se restringen longitud, URLs, correos y teléfonos",()=>{
  assert.equal(clean("   Mi   propuesta   ",95,8),"Mi propuesta");
  assert.throws(()=>clean("corto",95,8));
  assert.throws(()=>clean("x".repeat(96),95,8));
  assert.throws(()=>clean("Contactar por correo user@example.com",650,4));
  assert.throws(()=>clean("Mira https://example.com",650,4));
  assert.throws(()=>clean("Llamar +56 9 1234 5678",650,4));
  assert.equal(clean("Me gustaría mejorar el calendario",95,8),"Me gustaría mejorar el calendario");
});
test("identificadores opacos obligatorios",()=>{
  assert.equal(docId("abc_DEF-123456"),"abc_DEF-123456");
  for(const invalid of ["","abc","../../admin","a/b","!!!"])assert.throws(()=>docId(invalid));
});
