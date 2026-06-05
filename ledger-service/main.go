package main

import (
	"log"
	"net/http"
	"os"

	"github.com/gin-gonic/gin"
)

type LedgerRecord struct {
	PaymentID string  `json:"paymentId" binding:"required"`
	Amount    float64 `json:"amount"    binding:"required"`
	Status    string  `json:"status"    binding:"required"`
}

func main() {
	gin.SetMode(gin.ReleaseMode)

	r := gin.New()
	r.Use(gin.Recovery())
	r.Use(traceFlowMiddleware())

	r.GET("/health", func(c *gin.Context) {
		c.JSON(http.StatusOK, gin.H{"status": "ok", "service": "ledger-service"})
	})

	r.POST("/ledger/record", func(c *gin.Context) {
		var record LedgerRecord
		if err := c.ShouldBindJSON(&record); err != nil {
			c.JSON(http.StatusBadRequest, gin.H{"error": err.Error()})
			return
		}

		traceId, _ := c.Get("traceId")
		log.Printf("[ledger] trace=%s payment=%s amount=%.2f status=%s",
			traceId, record.PaymentID, record.Amount, record.Status)

		c.JSON(http.StatusCreated, gin.H{
			"status":    "recorded",
			"paymentId": record.PaymentID,
		})
	})

	port := os.Getenv("PORT")
	if port == "" {
		port = "8083"
	}

	log.Printf("[ledger] running on :%s", port)
	if err := r.Run(":" + port); err != nil {
		log.Fatal(err)
	}
}
