package com.plagiarism.db;

import com.plagiarism.model.DocumentModel;
import com.plagiarism.model.User;
import com.plagiarism.util.PasswordHasher;

import java.util.ArrayList;
import java.util.List;

/**
 * Provides initial seed data (users and reference corpus documents) for testing.
 */
public class DataSeeder {

    public static List<User> getSeedUsers() {
        List<User> users = new ArrayList<>();
        users.add(new User("usr_admin", "admin", "admin@plagiarism.org", PasswordHasher.hash("admin123"), "System Administrator"));
        users.add(new User("usr_student", "student", "student@university.edu", PasswordHasher.hash("student123"), "Alex Johnson"));
        users.add(new User("usr_prof", "professor", "prof@university.edu", PasswordHasher.hash("prof123"), "Dr. Robert Smith"));
        return users;
    }

    public static List<DocumentModel> getSeedDocuments() {
        List<DocumentModel> docs = new ArrayList<>();

        docs.add(new DocumentModel(
                "doc_ai_intro",
                "usr_admin",
                "Introduction to Artificial Intelligence and Machine Learning",
                "ai_intro.txt",
                "Artificial intelligence is a wide-ranging branch of computer science concerned with building smart machines capable of performing tasks that typically require human intelligence. Machine learning is a subset of artificial intelligence focused on building applications that learn from data and improve their accuracy over time without being explicitly programmed to do so. Supervised learning algorithms build a mathematical model of a set of data that contains both the inputs and the desired outputs. Unsupervised learning algorithms take a set of data that contains only inputs, and find structure in the data, like grouping or clustering of data points. Deep learning is part of a broader family of machine learning methods based on artificial neural networks with representation learning.",
                "Artificial Intelligence"
        ));

        docs.add(new DocumentModel(
                "doc_cloud_computing",
                "usr_admin",
                "Cloud Computing Architectures and Distributed Systems",
                "cloud_computing.txt",
                "Cloud computing is the on-demand availability of computer system resources, especially data storage and computing power, without direct active management by the user. Large clouds often have functions distributed over multiple locations, each of which is a data center. Cloud computing relies on sharing of resources to achieve coherence and economies of scale, typically using a pay-as-you-go model. Virtualization technologies allow hardware resources to be abstracted into scalable virtual machines. Containers, such as Docker, offer lightweight execution environments that share the host operating system kernel, providing high deployment efficiency and fast startup times across hybrid cloud environments.",
                "Cloud Computing"
        ));

        docs.add(new DocumentModel(
                "doc_data_structures",
                "usr_admin",
                "Data Structures and Algorithmic Complexity",
                "algorithms_complexity.txt",
                "In computer science, a data structure is a data organization, management, and storage format that enables efficient access and modification. More precisely, a data structure is a collection of data values, the relationships among them, and the functions or operations that can be applied to the data. Linear data structures include arrays, linked lists, stacks, and queues. Non-linear data structures include trees and graphs. Asymptotic analysis evaluates the time and space complexity of algorithms as input size grows toward infinity. Big O notation is the standard mathematical notation used to classify algorithms according to how their run time or space requirements grow as the input size increases.",
                "Computer Science"
        ));

        docs.add(new DocumentModel(
                "doc_mongodb_guide",
                "usr_admin",
                "NoSQL Databases and MongoDB Architecture",
                "mongodb_architecture.txt",
                "MongoDB is a source-available, cross-platform, document-oriented database program classified as a NoSQL database product. MongoDB uses JSON-like documents with optional schemas known as BSON. In MongoDB, records are documents stored within collections, rather than rows in relational tables. This flexible document model allows developers to store hierarchical data and evolve schemas easily over time. MongoDB supports secondary indexing, full text search, aggregation pipelines, and high availability through replica sets. Horizontal scalability is achieved through sharding, distributing data across multiple replica sets transparently to the application client.",
                "Database Systems"
        ));

        docs.add(new DocumentModel(
                "doc_cybersecurity",
                "usr_admin",
                "Fundamentals of Network Security and Cryptography",
                "cybersecurity_basics.txt",
                "Cybersecurity is the practice of protecting systems, networks, and programs from digital attacks. These cyberattacks are usually aimed at accessing, changing, or destroying sensitive information, extorting money from users via ransomware, or interrupting normal business processes. Cryptography is an indispensable tool in cybersecurity, providing confidentiality, data integrity, authentication, and non-repudiation. Symmetric encryption algorithms like AES use the same key for encryption and decryption, whereas asymmetric encryption like RSA uses public and private key pairs. Secure socket layer protocols ensure encrypted transport across the Internet.",
                "Cybersecurity"
        ));

        return docs;
    }
}
