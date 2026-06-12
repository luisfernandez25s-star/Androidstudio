const express = require('express');
const mongoose = require('mongoose');
const cors = require('cors');
require('dotenv').config();

const app = express();
const PORT = process.env.PORT || 3000;

app.use(cors());
app.use(express.json());

const mensajeSchema = new mongoose.Schema({
  usuario: { type: String, required: true },
  mensaje: { type: String, required: true },
  fecha: { type: String, required: true }
}, { timestamps: true });

const Mensaje = mongoose.model('Mensaje', mensajeSchema);

mongoose.connect(process.env.MONGO_URI)
  .then(() => console.log('MongoDB Atlas conectado'))
  .catch((error) => console.error('Error conectando MongoDB:', error));

app.get('/', (req, res) => {
  res.json({ ok: true, mensaje: 'API funcionando' });
});

app.post('/guardar', async (req, res) => {
  try {
    const { usuario, mensaje, fecha } = req.body;

    if (!usuario || !mensaje || !fecha) {
      return res.status(400).json({ ok: false, error: 'Faltan usuario, mensaje o fecha' });
    }

    const nuevoMensaje = await Mensaje.create({ usuario, mensaje, fecha });
    res.status(201).json({ ok: true, guardado: nuevoMensaje });
  } catch (error) {
    res.status(500).json({ ok: false, error: error.message });
  }
});

app.get('/mensajes', async (req, res) => {
  try {
    const mensajes = await Mensaje.find().sort({ createdAt: -1 }).limit(20);
    res.json({ ok: true, mensajes });
  } catch (error) {
    res.status(500).json({ ok: false, error: error.message });
  }
});

app.listen(PORT, () => console.log(`Servidor activo en puerto ${PORT}`));
