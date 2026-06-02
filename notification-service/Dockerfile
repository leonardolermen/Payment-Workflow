FROM node:20-alpine

WORKDIR /app
COPY sdks/sdk-node ./sdks/sdk-node
RUN cd sdks/sdk-node && npm install && npm run build
COPY notification-service ./notification-service

WORKDIR /app/notification-service

# The package.json references file:../sdks/sdk-node
RUN npm install

EXPOSE 8084
CMD ["npm", "start"]
