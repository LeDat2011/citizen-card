# Citizen Card Management System

A smart card-based citizen management system using JavaCard applet and JavaFX desktop application.

## Features

- **Smart Card Integration**: JavaCard 2.2.1 applet with AES-128 & RSA-1024 encryption
- **Desktop Application**: JavaFX-based UI with real-time card communication
- **Secure Authentication**: PIN-based authentication with MD5 hashing (5 retry attempts)
- **Photo Management**: Chunked photo transfer (8KB storage, 200-byte chunks)
- **Balance Operations**: Encrypted balance management with transaction logging
- **Database Integration**: H2 embedded database for card registry
- **Inline Validation**: Real-time form validation with user-friendly error messages

## Architecture

```
┌─────────────────────────────────────────┐
│  Desktop App (JavaFX)                   │
│  ┌───────────────────────────────────┐  │
│  │  Controllers (UI Layer)           │  │
│  │  └─> CardService                  │  │
│  └───────────────────────────────────┘  │
│              │                           │
│              │ javax.smartcardio         │
│              │ (ISO 7816-4 APDU)         │
│              ▼                           │
│  ┌───────────────────────────────────┐  │
│  │  H2 Database (Embedded)           │  │
│  │  - Card Registry                  │  │
│  │  - Transaction Logs               │  │
│  └───────────────────────────────────┘  │
└─────────────────────────────────────────┘
              │
              │ APDU Commands
              ▼
┌─────────────────────────────────────────┐
│  JavaCard Applet (Smart Card)          │
│  - AES-128 Encryption                  │
│  - RSA-1024 Signatures                 │
│  - PIN Management                      │
│  - Photo Storage (8KB)                 │
└─────────────────────────────────────────┘
```

## Project Structure

```
citizen_card/
├── applet/                          # JavaCard Applet
│   ├── src/citizen_applet/
│   │   └── citizen_applet.java     # Main applet (749 lines, all-in-one)
│   ├── bin/                         # Compiled .class and .cap files
│   └── applet.jcproj               # JCIDE project file
│
├── desktop/                         # JavaFX Desktop Application
│   ├── src/main/java/citizencard/
│   │   ├── controller/             # UI Controllers
│   │   │   ├── AdminDashboardController.java
│   │   │   ├── CitizenDashboardController.java
│   │   │   ├── DemoWorkflowController.java
│   │   │   ├── LoginViewController.java
│   │   │   └── PhotoManagementController.java
│   │   ├── service/                # Card communication
│   │   │   └── CardService.java
│   │   ├── dao/                    # Database access
│   │   │   └── CardDAO.java
│   │   ├── model/                  # Data models
│   │   └── util/                   # Utilities
│   │       ├── DatabaseViewer.java
│   │       ├── DialogUtils.java
│   │       ├── PhotoUtils.java
│   │       └── PinInputDialog.java
│   ├── src/main/resources/
│   │   └── css/
│   │       └── styles.css          # Unified design system (450 lines)
│   ├── data/                       # H2 Database (auto-generated)
│   │   ├── citizen_card.mv.db
│   │   └── citizen_card.trace.db
│   └── pom.xml                     # Maven configuration
│
├── README.md                        # This file
├── APPLET_STRUCTURE_GUIDE.md       # Applet architecture reference
├── APDU_COMMANDS_V2.md             # APDU command reference
├── DATABASE_VIEWER_GUIDE.md        # Database viewer guide
├── view-database.bat               # Database viewer (Windows)
└── view-database.sh                # Database viewer (Linux/Mac)
```

## Quick Start

### Prerequisites
- **Java 17+** (JDK 21 recommended)
- **Maven 3.6+**
- **JCIDE** (for JavaCard applet development)
- **Smart Card Reader** (for physical card testing)

### 1. Build Applet

```bash
# Using JCIDE
1. Open applet/applet.jcproj in JCIDE
2. Press F7 to build
3. Output: applet/bin/citizen_applet.cap

# Or using command line
cd applet
javac -g -target 1.2 -source 1.2 \
  -d bin \
  -classpath "path/to/api.jar" \
  src/citizen_applet/citizen_applet.java
```

### 2. Run Desktop Application

```bash
cd desktop
mvn clean compile
mvn javafx:run
```

### 3. View Database (Optional)

```bash
# Windows
view-database.bat

# Linux/Mac
./view-database.sh
```

## Usage

### Admin Dashboard
- Register new cards
- Upload citizen photos
- Manage card status
- View all registered cards

### Citizen Dashboard
- View card information
- Check balance
- View transaction history
- Update personal information

## Security Features

### Applet Security
- **PIN Authentication**: MD5-hashed PIN with 5 retry attempts
- **AES-128 Encryption**: All sensitive data encrypted (balance, personal info)
- **RSA-1024 Signatures**: Digital signatures for critical operations
- **Secure Key Storage**: Private keys never leave the card
- **Auto-deactivation**: Card locks after 5 failed PIN attempts

### Data Storage
- **Smart Card**: Encrypted personal data, balance, photo (8KB)
- **H2 Database**: Card registry, public keys, transaction logs
- **No Sensitive Data**: PIN and private keys never stored in database

## Technical Details

### JavaCard Applet
- **Version**: JavaCard 2.2.1
- **Crypto**: AES-128-ECB, RSA-1024, MD5
- **Storage**: 8KB photo buffer
- **Transfer**: Chunked transfer (200-byte chunks)
- **Size**: 749 lines (single file)

### Desktop Application
- **Framework**: JavaFX 20.0.1
- **Build Tool**: Maven 3.x
- **Database**: H2 (embedded)
- **Card I/O**: javax.smartcardio
- **Protocol**: ISO 7816-4 APDU

### Communication Protocol
```
Desktop App → javax.smartcardio → APDU Commands → JavaCard Applet
                                                         ↓
                                                   Card Memory
```

## APDU Commands

The applet supports the following INS codes (see [APDU_COMMANDS_V2.md](APDU_COMMANDS_V2.md) for details):

| INS | Command | Description |
|-----|---------|-------------|
| 0x50 | START_PHOTO_UPLOAD | Initialize photo upload session |
| 0x51 | UPLOAD_PHOTO_CHUNK | Upload photo chunk (200 bytes) |
| 0x52 | FINISH_PHOTO_UPLOAD | Finalize photo upload |
| 0x53 | GET_PHOTO_SIZE | Get stored photo size |
| 0x54 | GET_PHOTO_CHUNK | Download photo chunk |
| 0x20 | VERIFY_PIN | Authenticate with PIN |
| 0x21 | CHANGE_PIN | Change PIN |
| 0x22 | GET_BALANCE | Get encrypted balance |
| 0x23 | UPDATE_BALANCE | Update balance (topup/payment) |
| 0x24 | GET_CARD_ID | Get card identifier |
| 0x25 | GET_PUBLIC_KEY | Export RSA public key |
| 0x26 | SIGN_DATA | Create RSA signature |

## Documentation

- **[APPLET_STRUCTURE_GUIDE.md](APPLET_STRUCTURE_GUIDE.md)** - Applet architecture and code structure
- **[APDU_COMMANDS_V2.md](APDU_COMMANDS_V2.md)** - Complete APDU command reference
- **[DATABASE_VIEWER_GUIDE.md](DATABASE_VIEWER_GUIDE.md)** - Database viewer usage guide

## Troubleshooting

### Applet Build Errors
- Ensure only `citizen_applet.java` exists in `applet/src/citizen_applet/`
- Check JavaCard API path in classpath
- Verify JavaCard 2.2.1 compatibility

### Desktop App Won't Start
- Run `mvn clean compile` to rebuild
- Check Java 17+ is installed
- Verify Maven dependencies are resolved

### Card Not Detected
- Check card reader connection
- Verify card is inserted properly
- Ensure JCIDE terminal is running (for simulation)

### Database Errors
- Database auto-creates on first run
- Location: `desktop/data/citizen_card.mv.db`
- To reset: Delete database files and restart app
- See [DATABASE_VIEWER_GUIDE.md](DATABASE_VIEWER_GUIDE.md) for details

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Authors

Educational project for learning JavaCard and smart card development.

## Acknowledgments

- JavaCard technology by Oracle
- JavaFX framework
- H2 Database Engine
