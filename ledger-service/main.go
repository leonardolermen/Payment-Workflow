package main

import (
	"encoding/json"
	"log"
	"net/http"
	"os"

	"github.com/traceflow/sdk-go"
)

type LedgerRecord struct {
	PaymentID string  `json:"paymentId"`
	Amount    float64 `json:"amount"`
	Status    string  `json:"status"`
}

func recordHandler(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodPost {
		http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
		return
	}

	var record LedgerRecord
	if err := json.NewDecoder(r.Body).Decode(&record); err != nil {
		http.Error(w, err.Error(), http.StatusBadRequest)
		return
	}

	log.Printf("Recording payment %s to immutable ledger", record.PaymentID)

	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(http.StatusCreated)
	json.NewEncoder(w).Encode(map[string]string{"status": "recorded"})
}

func main() {
	mux := http.NewServeMux()
	mux.HandleFunc("/ledger/record", recordHandler)

	// Wrap with TraceFlow middleware
	handler := traceflow.Middleware(mux)

	port := os.Getenv("PORT")
	if port == "" {
		port = "8083"
	}

	log.Printf("Ledger Service running on port %s", port)
	if err := http.ListenAndServe(":"+port, handler); err != nil {
		log.Fatal(err)
	}
}
