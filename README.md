# Citizen Card Management System

A smart card-based citizen management system using JavaCard applet and JavaFX desktop application.

## ðŸŒŸ Features

- **Smart Card Integration**: JavaCard 2.2.1 applet with AES-128 & RSA-1024 encryption
- **Desktop Application**: JavaFX-based UI with real-time card communication
- **Secure Authentication**: PIN-based authentication with MD5 hashing (5 retry attempts)
- **Photo Management**: Chunked photo transfer (8KB storage, 200-byte chunks)
- **Balance Operations**: Encrypted balance management with transaction logging
- **Database Integration**: H2 embedded database for card registry
- **Inline Validation**: Real-time form validation with user-friendly error messages

## ðŸ—ï¸ Architecture

```
â”Œâ”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”
â”‚  Desktop App (JavaFX)                   â”‚
â”‚  â”Œâ”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”  â”‚
â”‚  â”‚  Controllers (UI Layer)           â”‚  â”‚
â”‚  â”‚  â””â”€> CardService                  â”‚  â”‚
â”‚  â””â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”˜  â”‚
â”‚              â”‚                           â”‚
â”‚              â”‚ javax.smartcardio         â”‚
â”‚              â”‚ (ISO 7816-4 APDU)         â”‚
â”‚              â–¼                           â”‚
â”‚  â”Œâ”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”  â”‚
â”‚  â”‚  H2 Database (Embedded)           â”‚  â”‚
â”‚  â”‚  - Card Registry                  â”‚  â”‚
â”‚  â”‚  - Transaction Logs               â”‚  â”‚
â”‚  â””â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”˜  â”‚
â””â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”˜
              â”‚
              â”‚ APDU Commands
              â–¼
â”Œâ”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”
â”‚  JavaCard Applet (Smart Card)          â”‚
â”‚  - AES-128 Encryption                  â”‚
â”‚  - RSA-1024 Signatures                 â”‚
â”‚  - PIN Management                      â”‚
â”‚  - Photo Storage (8KB)                 â”‚
â””â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”˜
```

## ðŸ“¦ Project Structure

```
citizen_card/
â”œâ”€â”€ applet/                          # JavaCard Applet
â”‚   â”œâ”€â”€ src/citizen_applet/
â”‚   â”‚   â””â”€â”€ citizen_applet.java     # Main applet (749 lines, all-in-one)
â”‚   â”œâ”€â”€ bin/                         # Compiled .class and .cap files
â”‚   â””â”€â”€ applet.jcproj               # JCIDE project file
â”‚
â”œâ”€â”€ desktop/                         # JavaFX Desktop Application
â”‚   â”œâ”€â”€ src/main/java/citizencard/
â”‚   â”‚   â”œâ”€â”€ controller/             # UI Controllers
â”‚   â”‚   â”‚   â”œâ”€â”€ AdminDashboardController.java
â”‚   â”‚   â”‚   â”œâ”€â”€ CitizenDashboardController.java
â”‚   â”‚   â”‚   â”œâ”€â”€ DemoWorkflowController.java
â”‚   â”‚   â”‚   â”œâ”€â”€ LoginViewController.java
â”‚   â”‚   â”‚   â””â”€â”€ PhotoManagementController.java
â”‚   â”‚   â”œâ”€â”€ service/                # Card communication
â”‚   â”‚   â”‚   â””â”€â”€ CardService.java
â”‚   â”‚   â”œâ”€â”€ dao/                    # Database access
â”‚   â”‚   â”‚   â””â”€â”€ CardDAO.java
â”‚   â”‚   â”œâ”€â”€ model/                  # Data models
â”‚   â”‚   â””â”€â”€ util/                   # Utilities
â”‚   â”‚       â”œâ”€â”€ DatabaseViewer.java
â”‚   â”‚       â”œâ”€â”€ DialogUtils.java
â”‚   â”‚       â”œâ”€â”€ PhotoUtils.java
â”‚   â”‚       â””â”€â”€ PinInputDialog.java
â”‚   â”œâ”€â”€ src/main/resources/
â”‚   â”‚   â””â”€â”€ css/
â”‚   â”‚       â””â”€â”€ styles.css          # Unified design system (450 lines)
â”‚   â”œâ”€â”€ data/                       # H2 Database (auto-generated)
â”‚   â”‚   â”œâ”€â”€ citizen_card.mv.db
â”‚   â”‚   â””â”€â”€ citizen_card.trace.db
â”‚   â””â”€â”€ pom.xml                     # Maven configuration
â”‚
â”œâ”€â”€ README.md                        # This file
â”œâ”€â”€ APPLET_STRUCTURE_GUIDE.md       # Applet architecture reference
â”œâ”€â”€ APDU_COMMANDS_V2.md             # APDU command reference
â”œâ”€â”€ DATABASE_VIEWER_GUIDE.md        # Database viewer guide
â”œâ”€â”€ view-database.bat               # Database viewer (Windows)
â””â”€â”€ view-database.sh                # Database viewer (Linux/Mac)
```

## ðŸš€ Quick Start

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

## ðŸ’» Usage

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

## 🔐 Security Features

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

## ðŸ”§ Technical Details

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
Desktop App â†’ javax.smartcardio â†’ APDU Commands â†’ JavaCard Applet
                                                         â†“
                                                   Card Memory
```

## ðŸ“‹ APDU Commands

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

## ðŸ“š Documentation

- **[APPLET_STRUCTURE_GUIDE.md](APPLET_STRUCTURE_GUIDE.md)** - Applet architecture and code structure
- **[APDU_COMMANDS_V2.md](APDU_COMMANDS_V2.md)** - Complete APDU command reference
- **[DATABASE_VIEWER_GUIDE.md](DATABASE_VIEWER_GUIDE.md)** - Database viewer usage guide

## ðŸ› Troubleshooting

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

## ðŸ¤ Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## ðŸ“„ License

This project is licensed under the MIT License - see the LICENSE file for details.

## ðŸ‘¥ Authors

Educational project for learning JavaCard and smart card development.

## ðŸ™ Acknowledgments

- JavaCard technology by Oracle
- JavaFX framework
- H2 Database Engine


