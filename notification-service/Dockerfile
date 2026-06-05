FROM node:20-alpine

WORKDIR /app/notification-service

COPY notification-service/package.json .
RUN npm install

COPY notification-service/ .

EXPOSE 8084
CMD ["node", "index.js"]
