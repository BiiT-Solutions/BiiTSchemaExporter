package com.biit.persistence;

import com.biit.persistence.configuration.JpaSchemaExporterConfigurationReader;
import com.biit.persistence.logger.ExporterLogger;
import com.biit.utils.string.StringConverter;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.hibernate.tool.hbm2ddl.SchemaUpdate;
import org.hibernate.tool.schema.TargetType;

import java.io.*;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

@SuppressWarnings("rawtypes")
public class JpaSchemaExporter {
    private static final int ARG_OUTPUT_DIRECTORY = 0;
    private static final int ARG_OUTPUT_FILE = 1;
    private static final int ARG_PACKETS_TO_SCAN = 2;
    private static final int ARG_DATABASE_USER = 3;
    private static final int ARG_DATABASE_PASSWORD = 4;
    private static final int ARG_DATABASE_HOST = 5;
    private static final int ARG_DATABASE_PORT = 6;
    private static final int ARG_DATABASE_NAME = 7;
    private static final int ARG_SCRIPTS_TO_ADD = 8;
    private static final int ARG_CLASSES_TO_IGNORE_CREATE_DATABASE = 9;
    private static final int ARG_CLASSES_TO_IGNORE_UPDATE_DATABASE = 10;

    private Set<Class> classToPersist = new HashSet<>();
    ;

    private static String directory, outputFile, user, password, host, port, databaseName;
    private static String[] packetsToScan, scriptsToAdd, classesToIgnoreWhenCreatingDatabase, classesToIgnoreWhenUpdatingDatabase;

    public JpaSchemaExporter(String[] packagesName, String[] classesToIgnore) {
        try {
            Set<Class> classes = new HashSet<>();
            // Get classes in project.
            for (int i = 0; i < packagesName.length; i++) {
                for (Class clazz : getClassesInProject(packagesName[i])) {
                    classes.add(clazz);
                }
            }

            // Remove ignored classes.
            Set<String> classesToRemove = new HashSet<String>(Arrays.asList(classesToIgnore));
            for (String classToRemove : new HashSet<>(classesToRemove)) {
                for (Class<?> classAdded : new ArrayList<>(classes)) {
                    if (classAdded.getSimpleName().equals(classToRemove)) {
                        classes.remove(classAdded);
                    }
                }
            }

            // Add to hibernate configuration.
            for (Class clazz : classes) {
                classToPersist.add(clazz);
            }

            if (!classesToRemove.isEmpty()) {
                ExporterLogger.warning(JpaSchemaExporter.class.getName(), "Classes '" + classesToRemove + "' not found in packet. Not ignored.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            ExporterLogger.errorMessage(JpaSchemaExporter.class.getName(), e);
        }
    }

    /**
     * Utility method used to fetch Class list based on a package name.
     *
     * @param packageName should be the package containing your annotated beans.
     */
    private List<Class> getClassesInProject(String packageName) throws ClassNotFoundException {
        File directory = null;
        try {
            ClassLoader cld = getClassLoader();
            URL resource = getResource(packageName, cld);
            // Resource of the project.
            if (!resource.getPath().contains("jar!")) {
                directory = new File(resource.getFile());
                return collectClasses(packageName, directory);
            } else {
                // Class inside a dependency JAR.
                String jarPath = resource.getPath().substring("file:".length(), resource.getPath().indexOf("!"));
                return getClassesInJar(jarPath, packageName);
            }
        } catch (NullPointerException ex) {
            ex.printStackTrace();
            throw new ClassNotFoundException(packageName + " (" + directory + ") does not appear to be a valid package.");
        }
    }

    private ClassLoader getClassLoader() throws ClassNotFoundException {
        ClassLoader cld = Thread.currentThread().getContextClassLoader();
        if (cld == null) {
            throw new ClassNotFoundException("Can't get class loader.");
        }
        return cld;
    }

    private URL getResource(String packageName, ClassLoader cld) throws ClassNotFoundException {
        String path = packageName.replace('.', '/');
        URL resource = cld.getResource(path);
        if (resource == null) {
            throw new ClassNotFoundException("No resource for " + path);
        }
        return resource;
    }

    private List<Class> getClassesInJar(String jarPath, String packetName) {
        List<Class> classes = new ArrayList<>();
        try (JarFile jarFile = new JarFile(jarPath)) {
            Enumeration<JarEntry> entries = jarFile.entries();
            while (entries.hasMoreElements()) {
                JarEntry jarEntry = (JarEntry) entries.nextElement();
                if (jarEntry.getName().endsWith(".class")) {
                    String classFile = jarEntry.getName().replaceAll("/", "\\.");
                    if (classFile.startsWith(packetName)) {
                        try {
                            classes.add(Class.forName(classFile.substring(0, classFile.length() - ".class".length())));
                        } catch (ClassNotFoundException e) {
                            e.printStackTrace();
                            ExporterLogger.errorMessage(JpaSchemaExporter.class.getName(), e);
                        }
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            ExporterLogger.errorMessage(JpaSchemaExporter.class.getName(), e);
        }
        return classes;
    }

    private List<Class> collectClasses(String packageName, File directory) throws ClassNotFoundException {
        List<Class> classes = new ArrayList<Class>();
        if (directory != null && directory.exists()) {
            String[] files = directory.list();
            for (String fileName : files) {
                File subdirectory = new File(directory.getPath() + File.separator + fileName);
                if (fileName.endsWith(".class")) {
                    // removes the .class extension
                    classes.add(Class.forName(packageName + '.' + fileName.substring(0, fileName.length() - ".class".length())));
                } else if (subdirectory.isDirectory()) {
                    // Subpacket.
                    classes.addAll(collectClasses(packageName + '.' + fileName, subdirectory));
                }
            }
        }
        return classes;
    }

    /**
     * Create a script that can generate a database for the selected dialect
     *
     * @param dialect
     * @param directory
     */
    public void createDatabaseScript(HibernateDialect dialect, String directory, String outputFile, String host, String port, String username, String password,
                                     String databaseName, boolean onlyCreation) {
        MetadataSources metadata = new MetadataSources(new StandardServiceRegistryBuilder().applySetting("hibernate.hbm2ddl.auto", "create")
                .applySetting("hibernate.connection.driver_class", dialect.getDriver()).applySetting("hibernate.dialect", dialect.getDialectClass())
                .applySetting("hibernate.show_sql", "false").applySetting("hibernate.connection.driver_class", dialect.getDriver())
                .applySetting("hibernate.connection.url", "jdbc:mysql://" + host + ":" + port + "/" + databaseName)
                .applySetting("hibernate.connection.username", username).applySetting("hibernate.connection.password", password).build());

        for (Class clazz : classToPersist) {
            metadata.addAnnotatedClass(clazz);
        }

        SchemaExport export = new SchemaExport();
        export.setDelimiter(";");
        export.setOutputFile(directory + File.separator + outputFile);
        export.setFormat(true);
        // export.create(EnumSet.of(TargetType.DATABASE),
        // metadata.buildMetadata());
        export.execute(EnumSet.of(TargetType.SCRIPT), SchemaExport.Action.CREATE, metadata.buildMetadata());
    }

    public void updateDatabaseScript(HibernateDialect dialect, String outputDirectory, String host, String port, String username, String password,
                                     String databaseName) {

        MetadataSources metadata = new MetadataSources(new StandardServiceRegistryBuilder().applySetting("hibernate.hbm2ddl.auto", "update")
                .applySetting("hibernate.connection.driver_class", dialect.getDriver()).applySetting("hibernate.dialect", dialect.getDialectClass())
                .applySetting("hibernate.show_sql", "false").applySetting("hibernate.connection.driver_class", dialect.getDriver())
                .applySetting("hibernate.connection.url", "jdbc:mysql://" + host + ":" + port + "/" + databaseName)
                .applySetting("hibernate.connection.username", username).applySetting("hibernate.connection.password", password).build());

        SchemaUpdate update = new SchemaUpdate();
        update.setDelimiter(";");

        File directory = new File(outputDirectory + File.separator + "updates");
        if (!directory.exists()) {
            directory.mkdirs();
        }

        update.setOutputFile(outputDirectory + File.separator + "updates" + File.separator + "update_" + dialect.name().toLowerCase() + "_" + getDate()
                + ".sql");
        update.setFormat(true);
        update.execute(EnumSet.of(TargetType.SCRIPT), metadata.buildMetadata());
    }

    /**
     * For executing.
     *
     * @param args args[{@value #ARG_OUTPUT_DIRECTORY}] -> outputDirectory, args[
     *             {@value #ARG_OUTPUT_FILE}] -> outputFile, args[
     *             {@value #ARG_PACKETS_TO_SCAN}] -> packetsToScan args[
     *             {@value #ARG_DATABASE_USER}] -> databaseUser, args[
     *             {@value #ARG_DATABASE_PASSWORD}] ->databasePassword, args[
     *             {@value #ARG_DATABASE_HOST}] -> databaseHost, args[
     *             {@value #ARG_DATABASE_PORT}] -> databasePort, args[
     *             {@value #ARG_CLASSES_TO_IGNORE_CREATE_DATABASE}] ->
     *             ignoreClassesCreating, args[
     *             {@value #ARG_CLASSES_TO_IGNORE_UPDATE_DATABASE}] ->
     *             ignoreClassesUpdates
     */
    public static void main(String[] args) {
        setArguments(args);

        // Launch the JpaSchemaExporter
        JpaSchemaExporter gen = new JpaSchemaExporter(getPacketsToScan(), getClassesToIgnoreWhenCreatingDatabase());
        gen.createDatabaseScript(HibernateDialect.MYSQL, getDirectory(), getOutputFile(), getHost(), getPort(), getUser(), getPassword(), getDatabaseName(),
                true);
        gen = new JpaSchemaExporter(getPacketsToScan(), getClassesToIgnoreWhenUpdatingDatabase());
        gen.updateDatabaseScript(HibernateDialect.MYSQL, getDirectory(), getHost(), getPort(), getUser(), getPassword(), getDatabaseName());

        // Add hibernate sequence table.
        //addTextToFile(createHibernateSequenceTable(), getDirectory() + File.separator + getOutputFile());
        // Add extra information from a external script.
        addTextToFile(readFile(getScriptsToAdd(), StandardCharsets.UTF_8), getDirectory() + File.separator + getOutputFile());
    }

    protected static void setArguments(String[] args) {

        if (args.length <= ARG_OUTPUT_DIRECTORY) {
            directory = JpaSchemaExporterConfigurationReader.getInstance().getOutputDirectory();
        } else {
            directory = args[ARG_OUTPUT_DIRECTORY] + File.separator;
        }

        if (args.length <= ARG_OUTPUT_FILE) {
            outputFile = JpaSchemaExporterConfigurationReader.getInstance().getOutputFile();
        } else {
            outputFile = args[ARG_OUTPUT_FILE];
        }

        if (args.length <= ARG_PACKETS_TO_SCAN) {
            packetsToScan = JpaSchemaExporterConfigurationReader.getInstance().getPackageToScan();
        } else {
            packetsToScan = StringConverter.convertToArray(args[ARG_PACKETS_TO_SCAN]);
        }

        if (args.length <= ARG_DATABASE_USER) {
            user = JpaSchemaExporterConfigurationReader.getInstance().getDatabaseUser();
        } else {
            user = args[ARG_DATABASE_USER];
        }

        if (args.length < ARG_DATABASE_PASSWORD) {
            password = JpaSchemaExporterConfigurationReader.getInstance().getDatabasePassword();
        } else {
            password = args[ARG_DATABASE_PASSWORD];
        }

        if (args.length <= ARG_DATABASE_HOST) {
            host = JpaSchemaExporterConfigurationReader.getInstance().getDatabaseHost();
        } else {
            host = args[ARG_DATABASE_HOST];
        }

        if (args.length <= ARG_DATABASE_PORT) {
            port = JpaSchemaExporterConfigurationReader.getInstance().getDatabasePort();
        } else {
            port = args[ARG_DATABASE_PORT];
        }

        if (args.length <= ARG_DATABASE_NAME) {
            databaseName = JpaSchemaExporterConfigurationReader.getInstance().getDatabaseName();
        } else {
            databaseName = args[ARG_DATABASE_NAME];
        }

        if (args.length <= ARG_SCRIPTS_TO_ADD) {
            scriptsToAdd = JpaSchemaExporterConfigurationReader.getInstance().getScriptsToAdd();
        } else {
            scriptsToAdd = StringConverter.convertToArray(args[ARG_SCRIPTS_TO_ADD]);
        }

        if (args.length <= ARG_CLASSES_TO_IGNORE_CREATE_DATABASE) {
            classesToIgnoreWhenCreatingDatabase = JpaSchemaExporterConfigurationReader.getInstance().getClassesToIgnoreCreatingDatabase();
        } else {
            classesToIgnoreWhenCreatingDatabase = StringConverter.convertToArray(args[ARG_CLASSES_TO_IGNORE_CREATE_DATABASE]);
        }

        if (args.length <= ARG_CLASSES_TO_IGNORE_UPDATE_DATABASE) {
            classesToIgnoreWhenUpdatingDatabase = JpaSchemaExporterConfigurationReader.getInstance().getClassesToIgnoreUpdatingDatabase();
        } else {
            classesToIgnoreWhenUpdatingDatabase = StringConverter.convertToArray(args[ARG_CLASSES_TO_IGNORE_UPDATE_DATABASE]);
        }
    }

    private static String getDate() {
        Date ahora = new Date();
        SimpleDateFormat formateador = new SimpleDateFormat("dd-MM-yyyy_HHmmss");
        return formateador.format(ahora);
    }

    /**
     * Hibernate databases needs this table.
     *
     * @return
     */
    protected static String createHibernateSequenceTable() {
        String table = "\n\tCREATE TABLE `hibernate_sequence` (\n";
        table += "\t\t`next_val` bigint(20) DEFAULT NULL\n";
        table += "\t);\n\n";
        table += "\tLOCK TABLES `hibernate_sequence` WRITE;\n";
        table += "\tINSERT INTO `hibernate_sequence` VALUES (1);\n";
        table += "\tUNLOCK TABLES;\n";
        return table;
    }

    protected static String readFile(String[] files, Charset charset) {
        StringBuilder result = new StringBuilder("");
        for (String file : files) {
            result.append("\n");

            File fileResource = new File(file);
            while (!fileResource.exists()) {
                // If this class is in a library, in testing the resources file
                // is the home of the project. Add the
                // complete path.
                if (file.indexOf('/') == -1) {
                    break;
                }
                fileResource = new File(file.substring(file.indexOf('/') + 1, file.length()));
                ExporterLogger.warning(JpaSchemaExporter.class.getName(), "'" + file + "' not found! Using '" + fileResource.getAbsolutePath() + "' instead.");
            }
            try (Scanner scanner = new Scanner(fileResource, StandardCharsets.UTF_8.displayName())) {
                while (scanner.hasNextLine()) {
                    String line = scanner.nextLine();
                    result.append("\t").append(line).append("\n");
                }
                scanner.close();
            } catch (FileNotFoundException e) {
                ExporterLogger.errorMessage(JpaSchemaExporter.class.getName(), e);
            }
        }
        return result.toString();
    }

    protected static void addTextToFile(String text, String file) {
        try (PrintWriter out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(
                new FileOutputStream(file, true), StandardCharsets.UTF_8)))) {
            out.println(text);
        } catch (IOException e) {
            ExporterLogger.errorMessage(JpaSchemaExporter.class.getName(), e);
        }
    }

    protected Class getSchemaExporterClass() {
        return JpaSchemaExporter.class;
    }

    public static String getDirectory() {
        return directory;
    }

    public static String getOutputFile() {
        return outputFile;
    }

    public static String getUser() {
        return user;
    }

    public static String getPassword() {
        return password;
    }

    public static String getHost() {
        return host;
    }

    public static String getPort() {
        return port;
    }

    public static String[] getPacketsToScan() {
        return packetsToScan;
    }

    public static String[] getScriptsToAdd() {
        return scriptsToAdd;
    }

    public static String getDatabaseName() {
        return databaseName;
    }

    public static String[] getClassesToIgnoreWhenCreatingDatabase() {
        return classesToIgnoreWhenCreatingDatabase;
    }

    public static String[] getClassesToIgnoreWhenUpdatingDatabase() {
        return classesToIgnoreWhenUpdatingDatabase;
    }

    public static void setPacketsToScan(String[] packetsToScan) {
        JpaSchemaExporter.packetsToScan = packetsToScan;
    }

    public Set<Class> getClassToPersist() {
        return classToPersist;
    }

}
