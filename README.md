# SensitiveInfoScanner - Java Projects

### Prerequisites
    -- Clone your repo which you want to scan to any directory in your local machine. This will be treated as *PATH_1* in below commands
    -- Checkout to specific branch for which the scanner has to run on PATH_1
    -- Clone this repo to any other directory in your local machine. This will be treated as *PATH_2* in below commands

### Execution
    Step #1 : cd PATH_2/src/main/resources
    Step #2 : Verify below property files
                - ignore_paths.properties -> default paths are already added. Add or remove necessary paths
                - ignore_variables.properties -> Add regex patterns for the names of variables which you want to ignore.
                                                e.g. You don't want a variable with name password to come in the scanner result, unless it is getting logges somewhere.
                                                     Note: If it is getting used in some log statement then it will come whether you add it here or not.
                - log_patterns.properties -> This includes the pattern of log statements we used in our code to write the logs. 
                                            We need to add all possible patterns of logs here.
                                            Some default is already added. Add or modify as required.
                - scan_file_types.properties -> Extensions of file like .java, .properties etc. 
                                                Some default is already added. Add or modify as required.
                - sensitive_patterns.properties -> It should contain sensitive keywords for which scan is run.
                                                    Some default is already added. Add or modify as required.
    Step #3 : This tool can be run using both your favourite IDE or using java command.
                - IDE -> You need to set two environment variables (space separated)
                        <PATH_1> <destination>
                            *PATH_1* - this is path of your repo which you need to scan. which is PATH_1 as mentioned above.
                            *destination* - this is the path of report file which you want to generate
                        Then you can directly run main method inside Scanner.java to generate the report.
                - command line ->
                            prerequisites - maven path should be set
                            cd PATH_2
                            mvn clean package
                            java -jar target/SIS-1.0.jar /Users/asgupta6/code/cisco/storm-eng/ems-assurance ems-reports.txt

# What this tool does
    -- Java Files: It detects sensitive attributes in the Java class fields, checks if these attributes are logged, and reports them.
                    It also detects if any sensitive keyword are logged through out the code which you are scanning.
                    If any sensitive keyword is inside your class and you are using the object of class to log it somewhere, then you will find it in generated report.
    -- Other Files: It simply scans for sensitive keywords directly in the file and reports if any are found.

### Notes
    -- Below things in the report you can ignore
        - If the report is pointing to something partial match and it is not relevant then you can ignore it.
            e.g. sensitive word to match is "cert"
                Now the intention is to match the word certificate or cert. But it can also match certain.
                So in such scenarios it can be ignored.
          We cannot apply full word match otherwise it will ignore any valuable match which can be a possible leak like names "certif", "authcert" etc.