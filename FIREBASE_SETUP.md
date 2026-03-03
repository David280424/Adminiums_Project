# 🔥 Configuración de Firebase - Adminiums

## Estructura de Firestore

Crea las siguientes colecciones en Firebase Console (https://console.firebase.google.com):

---

### Colección: `usuarios`
Documento por cada usuario (ID = UID de Firebase Auth):

```json
{
  "uid": "UID_FIREBASE",
  "nombre": "Juan Pérez",
  "email": "juan@ejemplo.com",
  "rol": "residente",          // "residente" | "vigilante" | "admin"
  "unidad": "Depto 301",
  "balance": 1250.00,
  "proximoPago": 450.00,
  "fechaVencimiento": "15 Feb 2026"
}
```

### Colección: `visitantes`
```json
{
  "id": "auto-id",
  "nombre": "Luis Morales",
  "autorizadoPor": "Juan Pérez",
  "unidad": "Depto 301",
  "vigencia": "3 Feb 2026",
  "qrCode": "ADMINIUMS|Luis Morales|...",
  "timestamp": 1234567890,
  "validado": false
}
```

### Colección: `reservaciones`
```json
{
  "id": "auto-id",
  "amenidad": "Piscina",
  "residenteUid": "UID_FIREBASE",
  "residenteNombre": "Juan Pérez",
  "unidad": "Depto 301",
  "fecha": "24/02/2026",
  "horario": "08:00 - 10:00",
  "timestamp": 1234567890
}
```

### Colección: `pagos`
```json
{
  "id": "auto-id",
  "residenteUid": "UID_FIREBASE",
  "monto": 450.00,
  "fecha": "24/02/2026",
  "estado": "pagado"
}
```

---

## Pasos para configurar Firebase

### 1. Firebase Authentication
- Ve a Firebase Console → Authentication → Sign-in method
- Habilita "Email/Password"

### 2. Firestore Database
- Ve a Firebase Console → Firestore Database → Crear base de datos
- Empieza en modo producción o prueba

### 3. Reglas de Firestore (modo desarrollo)
```
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /{document=**} {
      allow read, write: if request.auth != null;
    }
  }
}
```

### 4. Crear usuarios de prueba

**En Firebase Auth**, crea estos correos:

| Email | Contraseña | Rol |
|-------|-----------|-----|
| residente@test.com | 123456 | residente |
| vigilante@test.com | 123456 | vigilante |
| admin@test.com | 123456 | admin |

**En Firestore**, para cada usuario crea un documento en la colección `usuarios` con el UID correspondiente:

#### Residente de prueba:
```json
{
  "uid": "<UID del usuario residente>",
  "nombre": "Juan Pérez",
  "email": "residente@test.com",
  "rol": "residente",
  "unidad": "Depto 301",
  "balance": 1250.00,
  "proximoPago": 450.00,
  "fechaVencimiento": "15 Feb 2026"
}
```

#### Vigilante de prueba:
```json
{
  "uid": "<UID del usuario vigilante>",
  "nombre": "Carlos López",
  "email": "vigilante@test.com",
  "rol": "vigilante",
  "unidad": "",
  "balance": 0,
  "proximoPago": 0,
  "fechaVencimiento": ""
}
```

#### Administrador de prueba:
```json
{
  "uid": "<UID del usuario admin>",
  "nombre": "María González",
  "email": "admin@test.com",
  "rol": "admin",
  "unidad": "Condominio Los Pinos",
  "balance": 0,
  "proximoPago": 0,
  "fechaVencimiento": ""
}
```

---

## Funcionalidades implementadas

✅ **Login por rol** (Residente / Vigilante / Admin)  
✅ **Firebase Authentication** con email/password  
✅ **Firestore** para almacenamiento de datos  
✅ **Dashboard Residente** - Estado de cuenta, saldo, fecha de vencimiento  
✅ **Generar QR** para visitantes con guardado en Firestore  
✅ **Reservar amenidades** (Piscina, Gimnasio, Salón, BBQ)  
✅ **Pagar cuota** con actualización de balance  
✅ **Validar QR** de visitantes con cámara (ZXing)  
✅ **Buscar residentes** por nombre o unidad  
✅ **Panel Admin** con estadísticas de pagos y ocupación de amenidades  
