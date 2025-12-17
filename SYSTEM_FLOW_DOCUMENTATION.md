# 📋 Citizen Card System - Tài liệu Luồng Hệ thống

## Tổng quan Kiến trúc

```mermaid
graph TB
    subgraph "Desktop Application"
        UI[JavaFX UI]
        LC[LoginViewController]
        AC[AdminDashboardController]
        CC[CitizenDashboardController]
        DWC[DemoWorkflowController]
    end
    
    subgraph "Business Logic"
        CS[CardService]
        DAO[CardDAO]
    end
    
    subgraph "Smart Card"
        APP[citizen_applet.java]
    end
    
    subgraph "Database"
        H2[(H2 Database)]
    end
    
    UI --> LC
    LC --> AC
    LC --> CC
    UI --> DWC
    
    AC --> DAO
    CC --> CS
    CC --> DAO
    DWC --> CS
    DWC --> DAO
    
    CS --> APP
    DAO --> H2
```

---

## 🔐 1. Luồng Đăng nhập (Authentication Flow)

### 1.1 Đăng nhập Admin
```mermaid
sequenceDiagram
    actor Admin
    participant LV as LoginViewController
    participant AC as AdminDashboardController
    
    Admin->>LV: Chọn "Quản trị viên"
    LV->>AC: new AdminDashboardController()
    AC->>Admin: Hiển thị Dashboard
```

### 1.2 Đăng nhập Cư dân (PIN)
```mermaid
sequenceDiagram
    actor Citizen
    participant LV as LoginViewController
    participant CS as CardService
    participant APP as Applet
    participant DAO as CardDAO
    participant CC as CitizenDashboard
    
    Citizen->>LV: Chọn "Đăng nhập cư dân"
    LV->>CS: connectToCard()
    CS->>APP: SELECT APPLET
    APP-->>CS: OK (9000)
    
    LV->>CS: getCardId()
    CS->>APP: GET_CARD_ID (00 02 00 0A)
    APP-->>CS: cardId bytes
    
    Citizen->>LV: Nhập PIN
    LV->>CS: verifyPin(pin)
    CS->>APP: VERIFY_PIN (00 00 04 00 [4byte PIN])
    
    alt PIN đúng
        APP-->>CS: 9000 (Success)
        LV->>DAO: isCardRegistered(cardId)
        LV->>CC: new CitizenDashboardController(cardService, cardId)
        CC->>CC: loadDataFromCard()
        CC->>CC: syncApprovedTopups()
        CC->>Citizen: Hiển thị Dashboard
    else PIN sai
        APP-->>CS: 63xx (Remaining tries)
        LV->>Citizen: "Sai PIN, còn X lần thử"
    end
```

---

## 💳 2. Luồng Tạo Thẻ Mới

```mermaid
sequenceDiagram
    actor Admin
    participant DWC as DemoWorkflowController
    participant CS as CardService
    participant APP as Applet
    participant DAO as CardDAO
    
    Admin->>DWC: Điền form thông tin + PIN
    DWC->>CS: connectToCard()
    CS->>APP: SELECT APPLET
    
    DWC->>DWC: Generate Card ID (UUID)
    DWC->>CS: initializeCard(pin, cardId)
    CS->>APP: CREATE_INIT (00 01 04 00 [PIN + cardId])
    Note over APP: Tạo RSA Key Pair<br/>Lưu PIN (MD5 hash)<br/>Lưu Card ID
    APP-->>CS: 9000 + PublicKey
    
    DWC->>CS: updatePersonalInfo(personalData)
    CS->>APP: UPDATE_INFO (00 03 05 07 [encrypted data])
    Note over APP: Encrypt với AES<br/>Lưu vào infoBuffer
    
    DWC->>DAO: registerCard(cardId, publicKey)
    DAO->>DAO: INSERT registered_cards
    
    DWC->>Admin: "Tạo thẻ thành công!"
```

### Cấu trúc dữ liệu lưu trữ

| Vị trí | Dữ liệu | Mã hóa |
|--------|---------|--------|
| **Applet** | PIN, Card ID, RSA Keys, Personal Info, Avatar, Balance | AES-128 |
| **Database** | card_id, public_key, status, timestamps | Không |

---

## 📄 3. Luồng Hóa đơn (Invoice System)

### 3.1 Admin gửi hóa đơn
```mermaid
sequenceDiagram
    actor Admin
    participant AC as AdminDashboard
    participant DAO as CardDAO
    
    Admin->>AC: showCitizenManagement()
    AC->>DAO: getAllCards()
    DAO-->>AC: List<CardRecord>
    AC->>Admin: Hiển thị danh sách cư dân
    
    Admin->>AC: Click "Gửi HĐ" cho cardId
    AC->>AC: showSendInvoiceDialog(cardId)
    Admin->>AC: Nhập amount, description
    AC->>DAO: createInvoice(cardId, amount, desc)
    DAO->>DAO: INSERT invoices (status='PENDING')
    AC->>Admin: "Đã gửi hóa đơn thành công"
```

### 3.2 Cư dân thanh toán hóa đơn
```mermaid
sequenceDiagram
    actor Citizen
    participant CC as CitizenDashboard
    participant DAO as CardDAO
    participant CS as CardService
    participant APP as Applet
    
    Citizen->>CC: showInvoices()
    CC->>DAO: getInvoicesByCardId(cardId)
    DAO-->>CC: List<InvoiceRecord>
    CC->>Citizen: Hiển thị danh sách HĐ
    
    Citizen->>CC: Click "Thanh toán" invoice
    CC->>CS: getBalance()
    CS->>APP: GET_BALANCE (00 02 00 0C)
    APP-->>CS: currentBalance
    
    alt Đủ số dư
        Citizen->>CC: Xác nhận thanh toán
        CC->>CS: makePayment(amount)
        CS->>APP: UPDATE_BALANCE (00 03 05 0C [02][amount])
        Note over APP: balance -= amount
        APP-->>CS: newBalance
        CC->>DAO: payInvoice(invoiceId)
        DAO->>DAO: UPDATE status='PAID'
        CC->>Citizen: "Thanh toán thành công"
    else Không đủ số dư
        CC->>Citizen: "Số dư không đủ"
    end
```

---

## 💰 4. Luồng Nạp tiền (Topup System)

### 4.1 Cư dân yêu cầu nạp tiền
```mermaid
sequenceDiagram
    actor Citizen
    participant CC as CitizenDashboard
    participant DAO as CardDAO
    
    Citizen->>CC: showTopup()
    CC->>Citizen: Hiển thị QR chuyển khoản
    Note over Citizen: Chuyển khoản ngân hàng<br/>Nội dung: cardId
    
    Citizen->>CC: Nhập số tiền đã CK
    CC->>CC: Verify PIN
    CC->>DAO: createTopupRequest(cardId, amount)
    DAO->>DAO: INSERT topup_requests (status='PENDING')
    CC->>Citizen: "Yêu cầu đã gửi, chờ Admin duyệt"
```

### 4.2 Admin duyệt yêu cầu
```mermaid
sequenceDiagram
    actor Admin
    participant AC as AdminDashboard
    participant DAO as CardDAO
    
    Admin->>AC: showTopupRequests()
    AC->>DAO: getPendingTopupRequests()
    DAO-->>AC: List<TopupRecord>
    AC->>Admin: Hiển thị danh sách chờ duyệt
    
    Admin->>AC: Click "Duyệt" request
    AC->>AC: approveTopup(request)
    AC->>DAO: approveTopupRequest(requestId)
    DAO->>DAO: UPDATE status='APPROVED'
    AC->>Admin: "Đã duyệt thành công"
```

### 4.3 Đồng bộ số dư khi cư dân đăng nhập
```mermaid
sequenceDiagram
    participant CC as CitizenDashboard
    participant DAO as CardDAO
    participant CS as CardService
    participant APP as Applet
    
    Note over CC: Constructor được gọi
    CC->>CC: loadDataFromCard()
    CC->>CC: syncApprovedTopups()
    
    CC->>DAO: getTopupRequestsByCardId(cardId)
    DAO-->>CC: List<TopupRecord>
    
    loop Mỗi request APPROVED
        CC->>CS: topupBalance(amount)
        CS->>APP: UPDATE_BALANCE (00 03 05 0C [01][amount])
        Note over APP: balance += amount
        APP-->>CS: newBalance
        CC->>DAO: markTopupAsSynced(requestId)
        DAO->>DAO: UPDATE status='SYNCED'
    end
    
    CC->>CC: currentBalance = newBalance
```

---

## ✏️ 5. Luồng Chỉnh sửa Thông tin

```mermaid
sequenceDiagram
    actor Citizen
    participant CC as CitizenDashboard
    participant CS as CardService
    participant APP as Applet
    
    Citizen->>CC: showEditProfile()
    CC->>Citizen: Hiển thị form chỉnh sửa
    
    Citizen->>CC: Nhập email/phone mới
    CC->>CC: Validate input
    
    Citizen->>CC: Nhập PIN xác thực
    CC->>CS: verifyPin(pin)
    CS->>APP: VERIFY_PIN (00 00 04 00 [PIN])
    APP-->>CS: 9000 OK
    
    CC->>CS: updatePersonalInfo(data)
    CS->>APP: UPDATE_INFO (00 03 05 07 [encrypted])
    Note over APP: Encrypt với AES<br/>Cập nhật infoBuffer
    APP-->>CS: 9000 OK
    
    CC->>Citizen: "Cập nhật thành công"
```

---

## 🔄 6. Luồng Đổi PIN

```mermaid
sequenceDiagram
    actor User
    participant UI as Dashboard
    participant CS as CardService
    participant APP as Applet
    
    User->>UI: Đổi PIN
    User->>UI: Nhập PIN cũ, PIN mới
    
    UI->>CS: changePin(oldPin, newPin)
    CS->>APP: UPDATE_PIN (00 03 04 00 [old:4][new:4])
    
    Note over APP: 1. Verify old PIN<br/>2. Decrypt data với old key<br/>3. Update PIN hash<br/>4. Re-encrypt với new key
    
    alt Thành công
        APP-->>CS: 9000
        UI->>User: "Đổi PIN thành công"
    else PIN cũ sai
        APP-->>CS: 6983
        UI->>User: "PIN cũ không đúng"
    end
```

---

## 📊 Bảng Database Schema

```sql
-- Thẻ đăng ký (Chỉ lưu ID và Key)
CREATE TABLE registered_cards (
    id INTEGER PRIMARY KEY AUTO_INCREMENT,
    card_id TEXT UNIQUE NOT NULL,
    public_key TEXT NOT NULL,
    card_status TEXT DEFAULT 'ACTIVE',
    registered_at DATETIME,
    last_accessed DATETIME
);

-- Hóa đơn
CREATE TABLE invoices (
    id INTEGER PRIMARY KEY AUTO_INCREMENT,
    card_id TEXT NOT NULL,
    amount BIGINT NOT NULL,
    description TEXT,
    status TEXT DEFAULT 'PENDING', -- PENDING, PAID, CANCELLED
    created_at DATETIME
);

-- Yêu cầu nạp tiền
CREATE TABLE topup_requests (
    id INTEGER PRIMARY KEY AUTO_INCREMENT,
    card_id TEXT NOT NULL,
    amount BIGINT NOT NULL,
    status TEXT DEFAULT 'PENDING', -- PENDING, APPROVED, REJECTED, SYNCED
    created_at DATETIME,
    approved_at DATETIME
);

-- Log giao dịch
CREATE TABLE transaction_logs (
    id INTEGER PRIMARY KEY AUTO_INCREMENT,
    card_id TEXT NOT NULL,
    operation_type TEXT NOT NULL,
    timestamp DATETIME,
    success BOOLEAN,
    error_message TEXT
);
```

---

## 🔐 APDU Commands Reference

| INS | P1 | P2 | Chức năng | Data |
|-----|----|----|-----------|------|
| 00 | 04 | 00 | VERIFY_PIN | [4-byte PIN] |
| 01 | 04 | 00 | CREATE_INIT | [PIN:4][idLen:1][id:N] |
| 01 | 05 | 09 | CREATE_AVATAR | [totalLen:2][offset:2][data] |
| 01 | 06 | 00 | CREATE_SIGNATURE | [challenge data] |
| 02 | 00 | 0A | GET_CARD_ID | - |
| 02 | 00 | 0B | GET_PUBLIC_KEY | - |
| 02 | 00 | 0C | GET_BALANCE | - |
| 02 | 05 | 07 | GET_INFO | - |
| 02 | 05 | 09 | GET_AVATAR | - |
| 03 | 04 | 00 | UPDATE_PIN | [old:4][new:4] |
| 03 | 05 | 07 | UPDATE_INFO | [encrypted data] |
| 03 | 05 | 0C | UPDATE_BALANCE | [type:1][amount:4] |
| 03 | 0A | 00 | FORGET_PIN | [newPin:4] |
| 03 | 0B | 00 | ACTIVATE_CARD | - |
| 03 | 0C | 00 | DEACTIVATE_CARD | - |
| 10 | 00 | 00 | RESET_PIN_TRIES | - |
| 11 | 00 | 00 | CLEAR_CARD | - |

---

## 📁 Cấu trúc Project

```
citizen_card/
├── applet/
│   └── src/citizen_applet/
│       └── citizen_applet.java     # JavaCard Applet (866 lines)
├── desktop/
│   └── src/main/java/citizencard/
│       ├── MainApp.java            # Entry point
│       ├── controller/
│       │   ├── LoginViewController.java
│       │   ├── AdminDashboardController.java   # 1615 lines
│       │   ├── CitizenDashboardController.java # 1848 lines
│       │   └── DemoWorkflowController.java
│       ├── dao/
│       │   └── CardDAO.java        # 631 lines, H2 Database
│       ├── service/
│       │   └── CardService.java    # 1007 lines, APDU communication
│       └── util/
│           ├── RSAUtils.java       # RSA signature verification
│           ├── PinInputDialog.java # PIN dialog component
│           └── DataValidator.java
└── data/
    └── citizen_card.mv.db          # H2 Database file
```
