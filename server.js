const express = require("express");
const cors = require("cors");
const path = require("path");

const app = express();

const PORT = process.env.PORT || 10000;
const HOST = "0.0.0.0";

app.use(cors());
app.use(express.json());
app.use(express.urlencoded({ extended: true }));

// Servir index.html
app.use(express.static(path.join(__dirname)));

// --------------------------------------------------
// CONFIGURATION ARIS TV PRO
// --------------------------------------------------

const config = {
  appName: "ARIS TV PRO",
  version: "1.0.0",

  dns: [
    "https://example-dns-1.com",
    "https://example-dns-2.com",
    "https://example-dns-3.com"
  ]
};

// --------------------------------------------------
// UTILISATEURS DE TEST
// --------------------------------------------------

const users = [
  {
    id: 1,
    username: "admin",
    password: "admin123",
    active: true
  }
];

// --------------------------------------------------
// CODES D'ACTIVATION DE TEST
// --------------------------------------------------

const activationCodes = [
  {
    code: "ARIS-2026-0001",
    active: true,
    used: false,
    username: null
  }
];

// --------------------------------------------------
// APPAREILS
// --------------------------------------------------

const devices = [];

// --------------------------------------------------
// PAGE D'ACCUEIL
// --------------------------------------------------

app.get("/", (req, res) => {
  res.sendFile(path.join(__dirname, "index.html"));
});

// --------------------------------------------------
// TEST SERVEUR
// --------------------------------------------------

app.get("/api/health", (req, res) => {
  res.json({
    success: true,
    app: config.appName,
    version: config.version,
    status: "online",
    time: new Date().toISOString()
  });
});

// --------------------------------------------------
// CONFIGURATION APPLICATION
// --------------------------------------------------

app.get("/api/config", (req, res) => {
  res.json({
    success: true,
    appName: config.appName,
    version: config.version,
    dns: config.dns
  });
});

// --------------------------------------------------
// CONNEXION
// --------------------------------------------------

app.post("/api/login", (req, res) => {
  const { username, password } = req.body;

  if (!username || !password) {
    return res.status(400).json({
      success: false,
      message: "Username et password obligatoires"
    });
  }

  const user = users.find(
    (item) =>
      item.username === username &&
      item.password === password
  );

  if (!user) {
    return res.status(401).json({
      success: false,
      message: "Identifiants incorrects"
    });
  }

  if (!user.active) {
    return res.status(403).json({
      success: false,
      message: "Compte désactivé"
    });
  }

  res.json({
    success: true,
    message: "Connexion réussie",
    user: {
      id: user.id,
      username: user.username
    }
  });
});

// --------------------------------------------------
// ACTIVATION PAR CODE
// --------------------------------------------------

app.post("/api/activate", (req, res) => {
  const { code, username } = req.body;

  if (!code || !username) {
    return res.status(400).json({
      success: false,
      message: "Code et username obligatoires"
    });
  }

  const activation = activationCodes.find(
    (item) => item.code === code
  );

  if (!activation) {
    return res.status(404).json({
      success: false,
      message: "Code d'activation invalide"
    });
  }

  if (!activation.active) {
    return res.status(403).json({
      success: false,
      message: "Code désactivé"
    });
  }

  if (activation.used) {
    return res.status(403).json({
      success: false,
      message: "Code déjà utilisé"
    });
  }

  activation.used = true;
  activation.username = username;

  res.json({
    success: true,
    message: "Activation réussie",
    username
  });
});

// --------------------------------------------------
// ENREGISTRER UN APPAREIL
// --------------------------------------------------

app.post("/api/devices", (req, res) => {
  const {
    username,
    deviceId,
    deviceName
  } = req.body;

  if (!username || !deviceId) {
    return res.status(400).json({
      success: false,
      message: "Username et deviceId obligatoires"
    });
  }

  const existingDevice = devices.find(
    (device) => device.deviceId === deviceId
  );

  if (existingDevice) {
    return res.json({
      success: true,
      message: "Appareil déjà enregistré",
      device: existingDevice
    });
  }

  const device = {
    id: devices.length + 1,
    username,
    deviceId,
    deviceName: deviceName || "Android TV",
    createdAt: new Date().toISOString(),
    active: true
  };

  devices.push(device);

  res.json({
    success: true,
    message: "Appareil enregistré",
    device
  });
});

// --------------------------------------------------
// LISTE DES APPAREILS
// --------------------------------------------------

app.get("/api/devices/:username", (req, res) => {
  const username = req.params.username;

  const userDevices = devices.filter(
    (device) => device.username === username
  );

  res.json({
    success: true,
    devices: userDevices
  });
});

// --------------------------------------------------
// ADMIN : STATISTIQUES
// --------------------------------------------------

app.get("/api/admin/stats", (req, res) => {
  res.json({
    success: true,
    users: users.length,
    devices: devices.length,
    activationCodes: activationCodes.length,
    usedCodes: activationCodes.filter(
      (code) => code.used
    ).length
  });
});

// --------------------------------------------------
// ADMIN : LISTE UTILISATEURS
// --------------------------------------------------

app.get("/api/admin/users", (req, res) => {
  res.json({
    success: true,
    users: users.map((user) => ({
      id: user.id,
      username: user.username,
      active: user.active
    }))
  });
});

// --------------------------------------------------
// ADMIN : AJOUTER UN CODE
// --------------------------------------------------

app.post("/api/admin/activation-codes", (req, res) => {
  const { code } = req.body;

  if (!code) {
    return res.status(400).json({
      success: false,
      message: "Code obligatoire"
    });
  }

  const exists = activationCodes.some(
    (item) => item.code === code
  );

  if (exists) {
    return res.status(409).json({
      success: false,
      message: "Ce code existe déjà"
    });
  }

  const newCode = {
    code,
    active: true,
    used: false,
    username: null
  };

  activationCodes.push(newCode);

  res.json({
    success: true,
    message: "Code créé",
    code: newCode
  });
});

// --------------------------------------------------
// 404 API
// --------------------------------------------------

app.use("/api/*splat", (req, res) => {
  res.status(404).json({
    success: false,
    message: "API endpoint introuvable"
  });
});

// --------------------------------------------------
// ERREUR SERVEUR
// --------------------------------------------------

app.use((err, req, res, next) => {
  console.error(err);

  res.status(500).json({
    success: false,
    message: "Erreur interne du serveur"
  });
});

// --------------------------------------------------
// DEMARRAGE
// --------------------------------------------------

app.listen(PORT, HOST, () => {
  console.log(
    ${config.appName} API running on http://${HOST}:${PORT}
  );
});
