const express = require('express');

const app = express();
app.use(express.json());

app.post('/notifications/send', (req, res) => {
  const { paymentId, payeeId } = req.body;
  console.log(`[NotificationService] Sending payment notification for payment ${paymentId} to user ${payeeId}`);

  setTimeout(() => {
    res.status(200).json({ status: 'sent', message: 'Notification sent successfully' });
  }, 100);
});

const port = process.env.PORT || 8084;
app.listen(port, () => {
  console.log(`Notification Service running on port ${port}`);
});
