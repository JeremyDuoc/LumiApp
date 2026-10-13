# Pruebas de Seguridad de Firebase — Lumi Bonds
**Módulo:** Bonds / Partner Hub  
**Fecha:** 12 de octubre de 2026  
**Auditor:** Mobile Application Security Reviewer  
**Entorno de ejecución:** Firebase Emulator Suite (Firestore v15.12.0, Node.js v24.11.0, JDK 21 OpenJDK)  
**Archivo de reglas auditado:** `firestore.rules` (168 líneas)

---

## 1. Resumen de Ejecución Automatizada

Se ejecutaron dos suites de pruebas unitarias sobre el emulador oficial de Firestore:

1. **Suite de Reglas Base (`tools/firestore-tests/rules.test.cjs`):** 10 pruebas ejecutadas, 10 pasadas.
2. **Suite de Auditoría de Seguridad Específica (`tools/firestore-tests/audit_security_verification.test.cjs`):** 5 pruebas de validación de vulnerabilidades ejecutadas, 5 comprobadas.

Comando ejecutado:
```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
npx firebase emulators:exec --config firebase.audit.json --only firestore "node --test tools/firestore-tests/audit_security_verification.test.cjs"
```

Resultado consola:
```text
✔ [P0-2 CONFIRMADO] Un participante revocado TODAVÍA puede leer los datos de salud del documento (2010.7395ms)
✔ [P0-3 CONFIRMADO] El futuro participante NO puede hacer get(linkRef) mientras está PENDING (80.015ms)
✔ [P1-5 CONFIRMADO] Cualquier usuario autenticado que acierte el código de 6 dígitos obtiene el linkId y ownerUid (72.4092ms)
✔ [FASE E - AISLAMIENTO] El participante B de un vínculo no puede leer el vínculo de C con el mismo titular (125.2984ms)
✔ [INTEGRIDAD GESTOS] Joiner no puede modificar lastOwnerCareAction para suplantar al owner (101.1691ms)
ℹ tests 5
ℹ suites 0
ℹ pass 5
ℹ fail 0
ℹ duration_ms 2769.7442
```

---

## 2. Detalle de Pruebas de Seguridad y Resultados

### Caso 1: Vulnerabilidad P0-2 — Fuga de Datos tras Revocación
- **Objetivo:** Demostrar que un participante revocado puede seguir obteniendo el documento `partner_links/{linkId}` con todos los datos clínicos del otro participante.
- **Preparación:**
  - Vínculo creado por `owner_A` con snapshot clínico (`currentPhase: 'FOLLICULAR'`, `currentMood: 'Sensitive'`).
  - `joiner_B` se une exitosamente (estado `ACTIVE`).
  - `owner_A` revoca el vínculo actualizando `status: 'REVOKED'`.
- **Acción ejecutada:** `joiner_B` realiza `getDoc(doc(db, 'partner_links/link_1'))`.
- **Resultado esperado bajo seguridad estricta:** `PERMISSION_DENIED`.
- **Resultado real observado:** `assertSucceeds` pasó sin error. El documento completo con `ownerSnapshot` fue entregado al usuario revocado.
- **Causa raíz en reglas:**
  ```javascript
  allow get: if request.auth != null && (
    resource.data.ownerUid == request.auth.uid ||
    resource.data.partnerUid == request.auth.uid
  );
  ```
  Las reglas no evalúan `resource.data.status == 'ACTIVE'`.
- **Clasificación:** **CRÍTICA (Vulnerabilidad confirmada empíricamente)**.

---

### Caso 2: Vulnerabilidad P0-3 — Fallo de Transacción de Canje de Invitación
- **Objetivo:** Demostrar que la llamada a `transaction.get(linkRef)` en `PartnerRepositoryImpl.kt:268` genera `PERMISSION_DENIED` antes de que el usuario sea registrado en el vínculo.
- **Preparación:**
  - `owner_A` crea un vínculo en estado `PENDING` (`partnerUid = null`).
- **Acción ejecutada:** Usuario autenticado `future_joiner` intenta hacer `getDoc(linkRef)`.
- **Resultado real observado:** Rechazado con `PERMISSION_DENIED` (`assertFails` pasó).
- **Impacto arquitectónico:**
  En el código de la app Android (`PartnerRepositoryImpl.kt`):
  ```kotlin
  firestore.runTransaction { transaction ->
      ...
      val currentLink = transaction.get(linkRef) // <-- RECHAZADO POR REGLAS
  ```
  Al ser rechazado el `get` por las reglas de seguridad, la transacción en Android lanza una excepción y aborta el canje de la invitación.
- **Clasificación:** **BLOQUEO CRÍTICO (Incompatibilidad reglas vs cliente)**.

---

### Caso 3: Vulnerabilidad P1-5 — Adivinanza de Códigos de Invitación
- **Objetivo:** Demostrar que un atacante autenticado puede descubrir invitaciones activas probando códigos de 6 dígitos.
- **Preparación:**
  - `owner_A` genera invitación con código `654321` en `partner_invitations/654321`.
- **Acción ejecutada:** `attacker_stranger` realiza `getDoc(doc(db, 'partner_invitations/654321'))`.
- **Resultado real observado:** `assertSucceeds` pasó. El atacante obtuvo `linkId` y `ownerUid`.
- **Causa raíz en reglas:**
  ```javascript
  allow get: if request.auth != null && (
    resource.data.ownerUid == request.auth.uid ||
    (resource.data.status == 'PENDING' && resource.data.expiresAtMs > request.time.toMillis())
  );
  ```
  Cualquier usuario autenticado puede leer cualquier documento de invitación siempre que acierte el código y esté `PENDING`. Al no haber rate-limiting en Firestore, un script automatizado puede probar códigos hasta encontrar invitaciones activas.
- **Clasificación:** **ALTO RIESGO**.

---

### Caso 4: Fase E — Aislamiento Multi-Vínculo
- **Objetivo:** Demostrar que un miembro del círculo no puede leer los vínculos de otros miembros vinculados con la misma persona.
- **Preparación:**
  - `owner_A` mantiene Vínculo 1 con `joiner_B` (Activo).
  - `owner_A` mantiene Vínculo 2 con `user_C` (Activo).
- **Acción ejecutada:** `joiner_B` intenta leer `partner_links/link_2_with_C`.
- **Resultado real observado:** Rechazado con `PERMISSION_DENIED` (`assertFails` pasó).
- **Conclusión:** **CORRECTO**. Existe aislamiento efectivo a nivel de regla: un usuario solo puede leer vínculos donde su propio UID coincide con `ownerUid` o `partnerUid`.

---

### Caso 5: Integridad de Gestos — Prevención de Suplantación de Identidad
- **Objetivo:** Verificar que un participante no pueda forjar un gesto simulando haber sido enviado por la otra persona.
- **Preparación:** Vínculo activo entre `owner_A` y `joiner_B`.
- **Acción ejecutada:** `joiner_B` intenta actualizar `lastOwnerCareAction`.
- **Resultado real observado:** Rechazado con `PERMISSION_DENIED` (`assertFails` pasó).
- **Conclusión:** **CORRECTO**. Las reglas validan adecuadamente que cada participante solo pueda actualizar su propio campo (`lastPartnerCareAction` para el joiner).
