# UPI Payment Gateway Simulator

A production-inspired **UPI Payment Gateway Simulator** built using **Spring Boot, React, and MySQL**.

This project simulates the core workflow of a modern UPI-based payment system including wallet management, bank account linking, money transfers, QR payments, transaction history, and user authentication.

The objective of this project is to understand how digital payment systems work internally while following enterprise software development practices.

---

## Project Status

> **Current Phase:** Project Initialization

This repository is being developed incrementally.

Each phase will introduce new production-ready features and will be committed separately to GitHub.

---

## Goals

- Learn enterprise backend development
- Build a fintech-grade portfolio project
- Practice secure authentication using JWT
- Simulate real-world UPI payment workflows
- Follow clean architecture and scalable design principles

---

# Tech Stack

## Frontend

- React 19
- Vite
- Tailwind CSS
- Axios
- React Router
- Recharts

## Backend

- Java 17
- Spring Boot 3.5
- Spring Security
- Spring Data JPA
- Hibernate
- JWT Authentication
- Maven

## Database

- MySQL 9.x

---

# Planned Project Structure

```
UPI-Payment-Gateway-Simulator
│
├── backend
│   ├── controller
│   ├── service
│   ├── repository
│   ├── entity
│   ├── dto
│   ├── security
│   ├── config
│   ├── exception
│   └── util
│
├── frontend
│   ├── src
│   ├── components
│   ├── pages
│   ├── hooks
│   ├── services
│   └── assets
│
├── screenshots
│
└── README.md
```

---

# Planned Features

## Authentication

- User Registration
- Secure Login
- JWT Authentication
- Refresh Tokens
- Role-based Authorization

---

## Wallet

- Wallet Creation
- Wallet Balance
- Transaction Ledger
- Wallet Freeze

---

## Bank Accounts

- Add Bank Account
- Delete Bank Account
- Primary Bank Selection
- Account Verification Simulation

---

## UPI

- Create UPI ID
- UPI Availability Check
- Default UPI ID

---

## Money Transfer

- Send Money
- Receive Money
- Transaction Status
- Daily Transfer Limits
- Duplicate Payment Protection

---

## QR Payments

- Generate QR Code
- Scan QR Code
- Instant Payments

---

## Request Money

- Create Payment Requests
- Accept Requests
- Reject Requests

---

## Dashboard

- Wallet Summary
- Transaction Analytics
- Monthly Reports
- Spending Insights

---

## Admin Panel

- User Management
- Transaction Monitoring
- Failed Payments
- Wallet Freeze
- Statistics

---

# Security Features

- JWT Authentication
- Password Encryption
- Role-Based Access Control
- Input Validation
- Global Exception Handling
- Secure REST APIs

---

# Planned Architecture

```
React Frontend
        │
 REST API (HTTPS)
        │
Spring Boot Backend
        │
Service Layer
        │
Repository Layer
        │
MySQL Database
```

---

# Screenshots

Screenshots will be added as development progresses.

---

# Future Enhancements

- Docker Support
- Docker Compose
- CI/CD Pipeline
- Swagger Documentation
- Email Notifications
- Redis Caching
- Rate Limiting
- Audit Logging
- Payment Refunds
- Fraud Detection Rules

---

# Development Roadmap

- ✅ Phase 1 – Project Setup
- ⏳ Phase 2 – Authentication
- ⏳ Phase 3 – User Profile
- ⏳ Phase 4 – Wallet
- ⏳ Phase 5 – Bank Accounts
- ⏳ Phase 6 – UPI IDs
- ⏳ Phase 7 – Money Transfer
- ⏳ Phase 8 – QR Payments
- ⏳ Phase 9 – Request Money
- ⏳ Phase 10 – Transaction History
- ⏳ Phase 11 – Dashboard
- ⏳ Phase 12 – Admin Dashboard
- ⏳ Phase 13 – Notifications
- ⏳ Phase 14 – Docker & Deployment

---

# License

This project is created for educational and portfolio purposes.

---

## ⭐ Support

If you find this project interesting, consider giving it a ⭐ on GitHub.
