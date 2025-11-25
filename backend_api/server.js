require('dotenv').config();
const express = require('express');
const cors = require('cors');
const http = require('http');
const { Server } = require("socket.io");

const authRoutes = require('./routes/authRoutes');
const dataRoutes = require('./routes/dataRoutes');
const bookingRoutes = require('./routes/bookingRoutes');
const tripRoutes = require('./routes/tripRoutes');
const tripDetailRoutes = require('./routes/tripDetailRoutes');
const paymentRoutes = require('./routes/paymentRoutes');
const pushRoutes = require('./routes/pushRoutes'); 

// const { expirePendingBookings, BOOKING_PENDING_TTL_MINUTES } = require('./services/bookingService');

const app = express();
const server = http.createServer(app);

const io = new Server(server, {
    cors: {
        origin: "*",
        methods: ["GET", "POST"]
    }
});

// Middleware
app.use(cors());
app.use(express.json());

// API Routes
app.use('/api', authRoutes);
app.use('/api', dataRoutes);
app.use('/api', bookingRoutes);
app.use('/api', tripRoutes);
app.use('/api', tripDetailRoutes);
app.use('/api', paymentRoutes);
app.use('/api', pushRoutes); 

// Socket.IO
const connectedUsers = new Map(); // Store user_id -> socket

io.on('connection', (socket) => {
    console.log('A user connected:', socket.id);

    socket.on('register', (userId) => {
        if (userId) {
            console.log(`User with ID ${userId} registered with socket ${socket.id}`);
            connectedUsers.set(userId, socket);
        }
    });

    socket.on('disconnect', () => {
        console.log('User disconnected:', socket.id);
        for (let [userId, userSocket] of connectedUsers.entries()) {
            if (userSocket === socket) {
                connectedUsers.delete(userId);
                break;
            }
        }
    });
});

// Global error handler (optional but good practice)
app.use((err, req, res, next) => {
    console.error(err.stack);
    res.status(500).send('Something broke!');
});

// Run background tasks
// const runBackgroundTasks = () => {
//     setInterval(async () => {
//         // console.log(`Running expirePendingBookings (TTL=${BOOKING_PENDING_TTL_MINUTES}min)`);
//         try {
//             const expiredCount = await expirePendingBookings();
//             if (expiredCount > 0) {
//                 console.log(`Expired ${expiredCount} pending bookings.`);
//                  // Notify relevant users if possible via WebSocket
//                  io.emit('bookings_expired', { count: expiredCount });
//             }
//         } catch (error) {
//             console.error("Error expiring bookings:", error);
//         }
//     }, 1 * 60 * 1000); // Run every 1 minute
// };

// Start Server
const PORT = process.env.PORT || 5000;
server.listen(PORT, () => {
    console.log(`Server is running on port ${PORT}`);
    // runBackgroundTasks();
});

// Export io and connectedUsers for use in other modules
module.exports = { io, connectedUsers };
