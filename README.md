EL Profile Activation Maven Extension
-------------------------------------

Allows profile to be activated using an expression language expression. Currently supports MVEL2 only. 
Extension hijacks property activation and tries to evaluate mvel expression first, if this is not successful
it passes control to original property activator.

In order to activate extension, you cannot include it into ```<build><extensions>``` element, because profile activation is done
before it would be activated. So you need to copy following files into *$MAVEN_HOME/lib/ext*:

* el-profile-activator-extension.jar (available in target directory)
* mvel2-${version} (available http://search.maven.org/#search%7Cgav%7C1%7Cg%3A%22org.mvel%22%20AND%20a%3A%22mvel2%22, substitute with latest version)  

To profit from new activation, add following in your pom.xml:

    <profile>
        <id>my-profile</id>
    
        <activation>
            <property>
            	<!-- mvel property name is obligatory -->
                <name>mvel</name>
                <value>isdef foo &amp;&amp; foo=="abc"</value>
            </property>
        </activation>
    </profile>            

Since not all property names are valid MVEL identifiers (e.g., an
identifier cannot contain a dot), an identifier for the properties
map can be specified with the special `mvel` property name as
`mvel(<properties-map-identifier>)`.  For example:

    <profile>
        <id>my-profile</id>
    
        <activation>
            <property>
                <!-- mvel property name is obligatory; identifier p is properties map -->
                <name>mvel(p)</name>
                <value>isdef foo.env &amp;&amp; p["foo.env"]=="abc"</value>
            </property>
        </activation>
    </profile>

A few examples (an MVEL cheatsheet)
-----------------------------------

* Check if *foo* and *bar* are defined and have same value 
 
		isdef foo &amp;&amp; isdef bar &amp;&amp; foo==bar
		
* Check if *foo* is defined while *bar* is not
		
	    isdef foo &amp;&amp; !isdef bar
	    
* Check if *foo* starts with *abc* or *baz* contains *xyz*
	
		isdef foo &amp;&amp; foo.startsWith("abc")) || (isdef baz &amp;&amp; baz.contains("xyz"))

* Check if *foo.env* equals *test* by accessing properties via *p* identifier specified in *name* element as *mvel(p)*
	
		isdef foo.env &amp;&amp; p["foo.env"] == "test"

Complete MVEL reference guide is available at http://mvel.codehaus.org/Language+Guide+for+2.0		
