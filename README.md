# Secure P2P Messenger

This project is a secure peer-to-peer (P2P) messaging application designed with end-to-end encryption to ensure privacy and security in communication. Below you'll find all the necessary information to get started with the project.

## Table of Contents
- [Introduction](#introduction)
- [Features](#features)
- [Installation](#installation)
- [Usage](#usage)
- [Architecture](#architecture)

## Introduction

Secure P2P Messenger is a desktop application developed using Java 21 and JavaFX. It provides a secure platform for users to exchange messages with end-to-end encryption guarantees, ensuring data privacy and security. This application also includes features such as long-term message storage and secure group conversations.

## Features

- **End-to-End Encryption**: Utilizes TLSv1.3 for secure message transmission.
- **Long-Term Message Storage**: Messages are encrypted and stored across multiple cloud providers using AES and Shamir's Secret Sharing scheme.
- **Group Conversations**: Secure group messaging with ciphertext-policy attribute-based encryption (CP-ABE).
- **User Interface**: Developed with JavaFX for a user-friendly experience.

## Installation

1. **Clone the repository:**

```bash
git clone https://github.com/yourusername/secure-p2p-messenger.git
cd secure-p2p-messenger
```

2. **Build the project:**

Use Maven to manage dependencies and build the project.

```bash
mvn clean install
```

Run the application:

```bash
java -jar target/secure-p2p-messenger.jar
```

## Usage

1. Start the Application:
Launch the application by running the command above. The main interface will load, displaying the chat list and options to start private or group chats.

2. Private Chat:
Select a contact to start a private conversation. All messages are encrypted and stored securely.

3. Group Chat:
Create or join a group chat. Messages are encrypted using CP-ABE, ensuring that only group members can access the content.

4. Long-Term Storage:
When the application closes, messages are encrypted and stored in cloud services (Google Drive, Dropbox, GitHub). The AES key is split and stored using Shamir's Secret Sharing.

## Architecture

**Basic Implementation**

  Transport Layer Security (TLSv1.3): Encrypts messages exchanged between peers.
  Certificates and Truststores: Each peer has a keystore and truststore for secure communication.

**Long-Term Message Storage**

  Advanced Encryption Standard (AES): Encrypts the message file.
  Shamir's Secret Sharing (SSS): Splits AES key into shares stored across multiple cloud providers.
  Cloud Storage: Integrates Google Drive, Dropbox, and GitHub for storing encrypted message files.

**Group Conversations**

  Ciphertext-Policy Attribute-Based Encryption (CP-ABE): Secures messages in group chats.
  Access Policies: Defines who can access the group chat based on attributes.

**User Interface**

  JavaFX: Provides a graphical interface for the messaging app.

## Contributors
Lúıs Viana (fc62516)
Guilherme Santos (fc62533)

Supervised by:
Prof. Dr. Bernardo Ferreira
Department of Informatics, Faculty of Sciences of the University of Lisbon
