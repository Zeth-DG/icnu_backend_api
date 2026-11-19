# Pruebas de API con Postman — ICNU

> **Qué es este archivo:** el extracto de [`GUIA_CORRECCIONES.md`](GUIA_CORRECCIONES.md) con las comprobaciones HTTP de las fases 4 a 7, para hacerlas en Postman en lugar de la terminal.

> **Antes de empezar:** la app debe estar arrancada con la configuración `api bootRun` (`DB_PASSWORD` y `JWT_SECRET` definidas) y con el código de la fase que vas a verificar ya aplicado.

## Índice

- [Preparación única](#preparación-única)
- [Verifica la Fase 4](#verifica-la-fase-4)
- [Verifica la Fase 5](#verifica-la-fase-5)
- [Verifica la Fase 6](#verifica-la-fase-6)
- [Verifica la Fase 7](#verifica-la-fase-7)
- [Orden de ejecución](#orden-de-ejecución)

---

## Preparación única

Las verificaciones de las fases 4 a 7 se hacen en **Postman**, no en PowerShell. No es un capricho: el token del login se reutiliza en casi todas las peticiones, y cada `id` que devuelve una petición alimenta la siguiente. En PowerShell eso obliga a encadenar variables en la misma terminal; en Postman viven en un *environment* y se guardan solas con dos líneas de JavaScript en la pestaña **Tests**.

Haz esta preparación **una vez** y reutilízala en todas las fases.

### 1. Crea el environment

`Environments → +` → nómbralo **ICNU Local** → añade estas variables en la columna **Initial value** y pulsa **Save** (Ctrl+S). Recuerda seleccionarlo arriba a la derecha antes de enviar nada.

| Variable | Valor inicial | Para qué |
|---|---|---|
| `baseUrl` | `http://localhost:8080` | Prefijo de todas las URLs |
| `adminEmail` | `admin@icnu.local` | Admin de bootstrap (Fase 5) |
| `adminPassword` | `AdminSegura123` | Igual que `BOOTSTRAP_ADMIN_PASSWORD` |
| `anaEmail` | `ana@mail.com` | Cuidadora de pruebas |
| `anaPassword` | `Secreta123` | |
| `betoEmail` | `beto@mail.com` | Segundo cuidador, para comprobar los `403` |
| `betoPassword` | `Secreta123` | |
| `tokenAdmin` | *(vacío)* | Lo escribe el script del login |
| `tokenAna` | *(vacío)* | |
| `tokenBeto` | *(vacío)* | |
| `usuarioAnaId` | *(vacío)* | Id que devuelve el registro |
| `usuarioBetoId` | *(vacío)* | |
| `rolAdminId` | *(vacío)* | Ids de la Fase 5 |
| `rolCuidadorId` | *(vacío)* | |
| `rolNuevoId` | *(vacío)* | El rol que creas en la verificación |
| `pacienteId` | *(vacío)* | Ids de las fases 6 y 7 |
| `bitacoraId` | *(vacío)* | |
| `accionId` | *(vacío)* | |

> Si arrancas la app en otro puerto (`--server.port=18080`), cambia solo `baseUrl`. Ese es el motivo de tener la variable.

### 2. Crea la colección y las carpetas

`Collections → +` → **ICNU API**. Dentro, crea estas carpetas (clic derecho → *Add folder*):

```text
ICNU API
├── 00 · Auth
├── 10 · Roles
├── 20 · Pacientes
├── 30 · Bitácoras
└── 40 · Acciones
```

Los nombres con número solo sirven para que el orden visual coincida con el orden de ejecución.

### 3. Autorización

Cada request que necesite token va con **Authorization → Bearer Token → `{{tokenAna}}`** (o el token que corresponda). Alternativa menos repetitiva: selecciona la carpeta → **Authorization** → Bearer Token → `{{tokenAna}}`; los requests de dentro lo heredan.

Dos avisos que explican el 90 % de los `401` en Postman ([Apéndice B de la guía](GUIA_CORRECCIONES.md#401-al-usar-el-token-en-postman)):

- Si un request tiene su propia pestaña *Authorization*, **sobrescribe** la de la carpeta. Para una ruta pública, pon `No Auth` explícito.
- Un `Authorization: Bearer` mal formado (sin el espacio, o con dos espacios) da `401` aunque el token sea correcto. Usa la pestaña, no escribas la cabecera a mano.

### 4. Los cuatro scripts que vas a reutilizar

Se pegan en la pestaña **Tests** del request y se ejecutan al recibir la respuesta.

**Guardar el token** (en cada request de login, cambiando el nombre):

```js
pm.test("200 OK", () => pm.response.to.have.status(200));

const body = pm.response.json();
pm.test("devuelve token", () => pm.expect(body.token).to.be.a("string").and.not.empty);

pm.environment.set("tokenAna", body.token);
```

**Guardar un id**:

```js
const item = pm.response.json();
pm.environment.set("pacienteId", item.id);
```

**Esperar un error de negocio** (401, 403 o 404 con `ProblemDetail`):

```js
pm.test("código esperado", () => pm.response.to.have.status(403));
pm.test("problem+json", () =>
  pm.expect(pm.response.headers.get("Content-Type"))
    .to.include("application/problem+json"));
```

**Esperar un 201**:

```js
pm.test("201 creado", () => pm.response.to.have.status(201));
```

> Los ids se guardan como **texto** en el environment. Cuando compares con un id del JSON (que es número), envuélvelo: `Number(pm.environment.get("pacienteId"))`.

### 5. Regla de orden

Siempre: **login → token guardado → resto**. Un token caduca a las 24 h (`app.jwt.expiration-hours`); si empiezas a recibir `401` después de un rato, vuelve a enviar el login. Reiniciar la app no invalida el token, pero cambiar `JWT_SECRET` sí.

### 6. Cuidado al repetir el registro

`POST /api/usuarios/registro` devuelve `201` la primera vez y `409 El correo electrónico ya está registrado` las siguientes. No es un fallo: o cambias el email en el body, o borras el usuario. Los scripts de esta guía asumen una base limpia (`usuarios` vacía) la primera vez.

---

## Verifica la Fase 4

```powershell
.\gradlew.bat compileJava
```

Debe terminar en `BUILD SUCCESSFUL` y la app debe arrancar con la configuración `api bootRun`. Esta verificación se hace con la colección del apartado [Postman — preparación única](#preparación-única).

> **Orden importante:** el registro y el login con un usuario real necesitan el rol `CUIDADOR` sembrado, y eso ocurre en la Fase 5. Aquí solo se comprueba lo que no depende de roles; los checks de registro, login y `403` están en la Verifica de la Fase 5.

**1. Login con credenciales inexistentes** — `00 · Auth → Login inexistente`

`POST {{baseUrl}}/api/auth/login` con:

```json
{ "email": "nadie@test.com", "contrasenia": "x" }
```

Pestaña **Tests**:

```js
pm.test("401", () => pm.response.to.have.status(401));
pm.test("problem+json", () =>
  pm.expect(pm.response.headers.get("Content-Type"))
    .to.include("application/problem+json"));
pm.test("mensaje único", () =>
  pm.expect(pm.response.json().detail).to.eql("Correo o contraseña incorrectos."));
```

Esperado: `401`. **No** `404` (ruta mal escrita) ni `500` (excepción sin manejar).

**2. Ruta protegida sin token** — `00 · Auth → Usuarios sin token`

`GET {{baseUrl}}/api/usuarios` con `Authorization → No Auth` explícito (si hereda el token de la carpeta, no estás probando nada).

Esperado: `401` con `Content-Type: application/problem+json`.

**3. Validación** — `00 · Auth → Registro inválido`

`POST {{baseUrl}}/api/usuarios/registro`:

```json
{ "nombre": "", "email": "no-es-email", "contrasenia": "123" }
```

Esperado: `400` con `detail` y un objeto `errores` con una clave por campo.

```js
pm.test("400", () => pm.response.to.have.status(400));
pm.test("errores por campo", () =>
  pm.expect(pm.response.json().errores).to.be.an("object").that.is.not.empty);
```

Si los tres pasan, la base de seguridad está terminada. El resto del flujo (registro → login → `403`) se completa al final de la Fase 5.

---

## Verifica la Fase 5

> El admin de bootstrap solo se crea si arrancas la app con `BOOTSTRAP_ADMIN_EMAIL` y `BOOTSTRAP_ADMIN_PASSWORD` definidas (paso 5.4). Es una operación de una sola vez.

**1. Login del admin** — `00 · Auth → Login admin`

`POST {{baseUrl}}/api/auth/login`

```json
{ "email": "{{adminEmail}}", "contrasenia": "{{adminPassword}}" }
```

Pestaña **Tests**:

```js
pm.test("200 OK", () => pm.response.to.have.status(200));

const body = pm.response.json();
pm.test("rol ADMIN", () => pm.expect(body.rol).to.eql("ADMIN"));
pm.environment.set("tokenAdmin", body.token);
```

Esperado: `200` y `tokenAdmin` guardado. Un `401` aquí significa que el bootstrap no se ejecutó (variables vacías al arrancar o admin inexistente).

**2. Listar roles** — `10 · Roles → Listar`

`GET {{baseUrl}}/api/roles` con Bearer `{{tokenAdmin}}`.

```js
pm.test("200 OK", () => pm.response.to.have.status(200));

const roles = pm.response.json();
const nombres = roles.map(r => r.nombre);

pm.test("roles semilla", () => {
  ["ADMIN", "CUIDADOR", "RED_APOYO"].forEach(n =>
    pm.expect(nombres).to.include(n));
});

pm.environment.set("rolAdminId", roles.find(r => r.nombre === "ADMIN").id);
pm.environment.set("rolCuidadorId", roles.find(r => r.nombre === "CUIDADOR").id);
```

Esperado: `200` con al menos `ADMIN`, `CUIDADOR`, `RED_APOYO`. Si llega vacío, `DataInitializer` no corrió.

**3. Registro de Ana** — `00 · Auth → Registro Ana`

`POST {{baseUrl}}/api/usuarios/registro` (sin token, `No Auth`):

```json
{ "nombre": "Ana Pérez", "email": "ANA@Mail.com", "contrasenia": "{{anaPassword}}" }
```

```js
pm.test("201 creado", () => pm.response.to.have.status(201));

const u = pm.response.json();
pm.test("email normalizado", () => pm.expect(u.email).to.eql("ana@mail.com"));
pm.test("rol CUIDADOR", () => pm.expect(u.rol).to.eql("CUIDADOR"));
pm.test("no expone contraseña", () => pm.expect(u).to.not.have.property("contrasenia"));

pm.environment.set("usuarioAnaId", u.id);
```

Esperado: `201`. Un `404 El rol con el nombre [CUIDADOR] no existe` significa que no hay semilla; un `409` significa que ya registraste a Ana (cambia el email o limpia la tabla).

**4. Login de Ana** — `00 · Auth → Login Ana`

`POST {{baseUrl}}/api/auth/login` con `{ "email": "ANA@mail.com", "contrasenia": "{{anaPassword}}" }`.

```js
pm.test("200 OK", () => pm.response.to.have.status(200));

const body = pm.response.json();
pm.test("rol CUIDADOR", () => pm.expect(body.rol).to.eql("CUIDADOR"));
pm.environment.set("tokenAna", body.token);
```

Esperado: `200`. Prueba la normalización: el registro se hizo con `ANA@Mail.com` y el login también funciona con mayúsculas.

**5. Ana intenta listar usuarios** — `00 · Auth → Usuarios con token Ana`

`GET {{baseUrl}}/api/usuarios` con Bearer `{{tokenAna}}`.

```js
pm.test("403 prohibido", () => pm.response.to.have.status(403));
pm.test("problem+json", () =>
  pm.expect(pm.response.headers.get("Content-Type"))
    .to.include("application/problem+json"));
```

Esperado: `403` (autenticada, pero no es admin). Un `401` significaría que el token no se está enviando.

**6. Crear un rol con el admin** — `10 · Roles → Crear (admin)`

`POST {{baseUrl}}/api/roles` con Bearer `{{tokenAdmin}}`:

```json
{ "nombre": "TERAPEUTA" }
```

```js
pm.test("201 creado", () => pm.response.to.have.status(201));
pm.environment.set("rolNuevoId", pm.response.json().id);
```

Esperado: `201`. Fíjate en que el servicio normaliza el nombre a mayúsculas.

**7. Crear un rol con el cuidador** — `10 · Roles → Crear (cuidador)`

El mismo `POST /api/roles` con Bearer `{{tokenAna}}` y `{ "nombre": "FISIOTERAPEUTA" }`.

Esperado: `403` y el rol **no** se crea (compruébalo volviendo a listar).

**8. Reinicio idempotente** — `10 · Roles → Listar`

Para la app, deja `BOOTSTRAP_ADMIN_EMAIL` y `BOOTSTRAP_ADMIN_PASSWORD` vacías y vuelve a arrancarla. Repite el listado con `{{tokenAdmin}}`.

```js
const nombres = pm.response.json().map(r => r.nombre);
pm.test("TERAPEUTA una sola vez", () =>
  pm.expect(nombres.filter(n => n === "TERAPEUTA")).to.have.lengthOf(1));
pm.test("ADMIN una sola vez", () =>
  pm.expect(nombres.filter(n => n === "ADMIN")).to.have.lengthOf(1));
```

Esperado: ni roles ni acciones duplicados. El admin sigue existiendo aunque hayas vaciado las variables: vaciarlas solo impide crear otro en el siguiente arranque.

**9. (Opcional) Borrar el rol de prueba** — `10 · Roles → Borrar`

`DELETE {{baseUrl}}/api/roles/{{rolNuevoId}}` con Bearer `{{tokenAdmin}}` → `204`. Después, `GET /api/roles/{{rolNuevoId}}` → `404`.

Checklist final de la fase:

- [ ] Login del admin devuelve `rol: "ADMIN"`.
- [ ] Los tres roles semilla existen una sola vez tras reiniciar.
- [ ] Registro de Ana devuelve `201`, email normalizado y sin contraseña.
- [ ] Login de Ana devuelve token.
- [ ] `GET /api/usuarios` con Ana → `403`; sin token → `401`.

---

## Verifica la Fase 6

Prerrequisitos: `tokenAna`, `tokenAdmin` (Fase 5) y un segundo cuidador con su token.

**0. Registra y loguea a Beto** — `00 · Auth → Registro Beto` / `Login Beto`

Registro: `{ "nombre": "Beto Ruiz", "email": "{{betoEmail}}", "contrasenia": "{{betoPassword}}" }` → `201`, guarda `usuarioBetoId`.

```js
pm.environment.set("usuarioBetoId", pm.response.json().id);
```

Login con `{{betoEmail}}` / `{{betoPassword}}` → `200`, guarda `tokenBeto`. Beto será el "otro cuidador" que debe recibir los `403`.

**1. Crear paciente con Ana** — `20 · Pacientes → Crear (Ana)`

`POST {{baseUrl}}/api/pacientes` con Bearer `{{tokenAna}}`:

```json
{
  "nombre": "Rosa Pérez",
  "fechaNacimiento": "1948-03-15",
  "alergias": "Penicilina",
  "enfermedades": "Hipertensión",
  "discapacidad": "Auditiva leve",
  "familiarEmergencia": "Ana Pérez",
  "medicoEmergencia": "Dra. Gómez"
}
```

```js
pm.test("201 creado", () => pm.response.to.have.status(201));

const p = pm.response.json();
pm.test("devuelve DTO plano", () => {
  pm.expect(p).to.have.property("id");
  pm.expect(p).to.not.have.property("cuidadores");
  pm.expect(p).to.not.have.property("bitacoras");
});
pm.environment.set("pacienteId", p.id);
```

Esperado: `201` y un JSON plano (sin `cuidadores` ni `usuario` anidados). Si ves un JSON enorme, el controller está devolviendo la entidad.

**2. Listar como Ana** — `20 · Pacientes → Listar (Ana)`

`GET {{baseUrl}}/api/pacientes` con Bearer `{{tokenAna}}`.

```js
pm.test("200 OK", () => pm.response.to.have.status(200));

const page = pm.response.json();
const ids = page.contenido.map(p => p.id);
pm.test("incluye el paciente creado", () =>
  pm.expect(ids).to.include(Number(pm.environment.get("pacienteId"))));
```

Ojo con los nombres: `PageResponse` usa `contenido`, `pagina`, `tamanio`, `totalElementos`, `totalPaginas`, no `content`.

**3. Obtener con Ana** — `20 · Pacientes → Obtener (Ana)`

`GET {{baseUrl}}/api/pacientes/{{pacienteId}}` con Bearer `{{tokenAna}}` → `200`.

**4. Beto intenta verlo** — `20 · Pacientes → Obtener (Beto)`

La misma URL con Bearer `{{tokenBeto}}` → `403` (no es cuidador de Rosa).

```js
pm.test("403", () => pm.response.to.have.status(403));
```

**5. Actualizar con Ana** — `20 · Pacientes → Actualizar (Ana)`

`PUT {{baseUrl}}/api/pacientes/{{pacienteId}}` con el mismo body del paso 1, cambiando `nombre` a `"Rosa Pérez de la Cruz"`.

```js
pm.test("200 OK", () => pm.response.to.have.status(200));
pm.test("cambió el nombre", () =>
  pm.expect(pm.response.json().nombre).to.eql("Rosa Pérez de la Cruz"));
```

Después repite el paso 3: debe seguir devolviendo `200`. Eso demuestra que el `PUT` **actualiza y no borra** (el bug original llamaba a `eliminarPaciente`).

**6. Ana intenta borrar** — `20 · Pacientes → Borrar (Ana)`

`DELETE {{baseUrl}}/api/pacientes/{{pacienteId}}` con Bearer `{{tokenAna}}` → `403`.

**7. Asignar a Beto y comprobar acceso** — `20 · Pacientes → Asignar cuidador`

`POST {{baseUrl}}/api/pacientes/{{pacienteId}}/cuidadores` con Bearer `{{tokenAdmin}}`:

```json
{ "usuarioId": {{usuarioBetoId}} }
```

Esperado: `200` con el paciente. Ahora repite el paso 4 (obtener con Beto): debe pasar de `403` a `200`.

```js
pm.test("200 tras asignar", () => pm.response.to.have.status(200));
pm.test("sin recursión JSON", () =>
  pm.expect(pm.response.json()).to.not.have.property("cuidadores"));
```

**8. Borrar como admin** — `20 · Pacientes → Borrar (admin)`

`DELETE {{baseUrl}}/api/pacientes/{{pacienteId}}` con Bearer `{{tokenAdmin}}` → `204`, y el `GET` posterior → `404`.

> El paciente queda borrado. La Fase 7 empieza creando uno nuevo, no reutilices `pacienteId`.

Checklist de la fase:

- [ ] `POST` devuelve `201` y un DTO plano.
- [ ] Ana ve su paciente; Beto recibe `403`.
- [ ] `PUT` actualiza y no borra.
- [ ] `DELETE` con Ana → `403`; con admin → `204`.
- [ ] Asignar a Beto le da acceso sin entrar en recursión JSON.

---

## Verifica la Fase 7

Prerrequisitos: `tokenAna`, `tokenBeto`, `tokenAdmin` y un paciente de Ana (el de la Fase 6 se borró).

**0. Prepara los datos**

- `20 · Pacientes → Crear (Ana)`: vuelve a crear el paciente y guarda el nuevo `pacienteId`.
- `40 · Acciones → Listar` con Bearer `{{tokenAna}}`: debe devolver `MEDICACION`, `CITA_MEDICA` y `NOTA_GENERAL`. Guarda una:

```js
const acciones = pm.response.json();
pm.environment.set("accionId", acciones[0].id);
```

**1. Crear bitácora con Ana** — `30 · Bitácoras → Crear (Ana)`

`POST {{baseUrl}}/api/bitacoras` con Bearer `{{tokenAna}}`:

```json
{
  "notas": "Se administró la medicación de las 8:00",
  "pacienteId": {{pacienteId}},
  "accionId": {{accionId}}
}
```

```js
pm.test("201 creado", () => pm.response.to.have.status(201));

const b = pm.response.json();
pm.test("guardó las notas", () =>
  pm.expect(b.notas).to.contain("medicación"));
pm.test("el autor sale del token", () =>
  pm.expect(b.usuarioId).to.eql(Number(pm.environment.get("usuarioAnaId"))));

pm.environment.set("bitacoraId", b.id);
```

Esperado: `201` y las notas **en la respuesta**. Aquí se comprueba el bug del `setNotas` que faltaba. `fecha` se puede omitir: el servidor pone la actual.

**2. Beto intenta leerla** — `30 · Bitácoras → Obtener (Beto)`

`GET {{baseUrl}}/api/bitacoras/{{bitacoraId}}` con Bearer `{{tokenBeto}}` → `403` (no es cuidador de ese paciente).

**3. Ana la edita y Beto no** — `30 · Bitácoras → Editar (Ana)`

`PUT {{baseUrl}}/api/bitacoras/{{bitacoraId}}` con Bearer `{{tokenAna}}` y el mismo body, cambiando `notas` a `"Nota editada por Ana"`:

```js
pm.test("200 OK", () => pm.response.to.have.status(200));
pm.test("notas actualizadas", () =>
  pm.expect(pm.response.json().notas).to.eql("Nota editada por Ana"));
```

Repite el `PUT` con Bearer `{{tokenBeto}}` → `403`.

**4. Ana la borra** — `30 · Bitácoras → Borrar (Ana)`

`DELETE {{baseUrl}}/api/bitacoras/{{bitacoraId}}` con Bearer `{{tokenAna}}` → `204`; después `GET` → `404`.

**5. Paciente inexistente** — `30 · Bitácoras → Crear (paciente inexistente)`

`POST /api/bitacoras` con `"pacienteId": 999999` y Bearer `{{tokenAna}}`.

```js
pm.test("404", () => pm.response.to.have.status(404));
pm.test("no es un 500", () => pm.expect(pm.response.code).to.not.eql(500));
```

Esperado: `404` con `problem+json`. Un `500` significa que una excepción sin traducir llegó al cliente.

**6. Borrar el paciente con bitácoras** — `20 · Pacientes → Borrar (admin)`

Vuelve a ejecutar el paso 1 (crear otra bitácora para el paciente) y luego `DELETE {{baseUrl}}/api/pacientes/{{pacienteId}}` con Bearer `{{tokenAdmin}}`.

Esperado: `204`, sin `Cannot delete or update a parent row: a foreign key constraint fails`. Un `GET /api/pacientes/{{pacienteId}}` posterior debe dar `404`.

Checklist de la fase:

- [ ] Crear bitácora devuelve `201` con `notas` y `usuarioId` del token.
- [ ] Beto recibe `403` al leer y al editar la bitácora de Ana.
- [ ] Ana puede editar y borrar la suya.
- [ ] `pacienteId` inexistente → `404`, no `500`.
- [ ] Borrar un paciente con bitácoras → `204` sin error de FK.

---

## Orden de ejecución

| # | Request | Carpeta | Deja listo |
|---|---|---|---|
| 1 | Login admin | `00 · Auth` | `tokenAdmin` |
| 2 | Listar roles | `10 · Roles` | `rolAdminId`, `rolCuidadorId` |
| 3 | Registro Ana | `00 · Auth` | `usuarioAnaId` |
| 4 | Login Ana | `00 · Auth` | `tokenAna` |
| 5 | Registro Beto | `00 · Auth` | `usuarioBetoId` |
| 6 | Login Beto | `00 · Auth` | `tokenBeto` |
| 7 | Crear paciente (Ana) | `20 · Pacientes` | `pacienteId` |
| 8 | Listar acciones | `40 · Acciones` | `accionId` |
| 9 | Crear bitácora (Ana) | `30 · Bitácoras` | `bitacoraId` |

> Si ejecutas la colección con el **Collection Runner**, respeta el orden de las carpetas. Los pasos que crean datos (registro, paciente, bitácora) devuelven `409` o `404` si los repites sin limpiar la base.

---

*Volver a la guía principal: [`GUIA_CORRECCIONES.md`](GUIA_CORRECCIONES.md).*