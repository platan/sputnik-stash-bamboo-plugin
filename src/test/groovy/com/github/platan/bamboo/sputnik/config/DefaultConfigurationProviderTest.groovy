package com.github.platan.bamboo.sputnik.config

import static com.atlassian.bamboo.variable.VariableType.GLOBAL
import static com.atlassian.bamboo.variable.VariableType.PLAN
import static com.github.platan.bamboo.sputnik.SputnikTaskConfigurationConstants.BUILDER_SPUTNIK_CONFIG_BODY

import com.atlassian.bamboo.configuration.ConfigurationMapImpl
import com.atlassian.bamboo.task.TaskException
import com.atlassian.bamboo.v2.build.agent.capability.CapabilityContext
import com.atlassian.bamboo.variable.VariableDefinitionContext
import com.atlassian.bamboo.variable.VariableType
import com.github.platan.bamboo.sputnik.SimpleVariableDefinitionContext
import spock.lang.Specification
import spock.lang.Unroll

class DefaultConfigurationProviderTest extends Specification {

    def 'get credentials from variables'() {
        given:
        def variableList = [aVariable('sputnik.connector.username', 'user1', GLOBAL),
                            aVariable('sputnik.connector.password', 'password1', GLOBAL)]
        Map<String, VariableDefinitionContext> variables = createVariables(variableList)
        DefaultConfigurationProvider configurationProvider = new DefaultConfigurationProvider()

        when:
        def configuration = configurationProvider.getConfiguration(variables, Stub(CapabilityContext), new ConfigurationMapImpl())

        then:
        with(configuration.credentials) {
            stashUsername == 'user1'
            stashPassword == 'password1'
        }
    }

    def 'prefer credentials from configuration over from variables'() {
        given:
        def variableList = [aVariable('sputnik.connector.username', 'user1', GLOBAL),
                            aVariable('sputnik.connector.password', 'password1', GLOBAL)]
        Map<String, VariableDefinitionContext> variables = createVariables(variableList)
        DefaultConfigurationProvider configurationProvider = new DefaultConfigurationProvider()
        String taskConfiguration = '''connector.username=user2
                                      connector.password=password2'''
        def configurationMap = new ConfigurationMapImpl([(BUILDER_SPUTNIK_CONFIG_BODY): taskConfiguration])

        when:
        def configuration = configurationProvider.getConfiguration(variables, Stub(CapabilityContext), configurationMap)

        then:
        with(configuration.credentials) {
            stashUsername == 'user2'
            stashPassword == 'password2'
        }
    }

    def 'get sputnik path from capability context'() {
        given:
        def variableList = [aVariable('sputnik.connector.username', 'user1', GLOBAL),
                            aVariable('sputnik.connector.password', 'password1', GLOBAL)]
        Map<String, VariableDefinitionContext> variables = createVariables(variableList)
        DefaultConfigurationProvider configurationProvider = new DefaultConfigurationProvider()
        def capabilityContext = Stub(CapabilityContext) {
            getCapabilityValue('system.builder.sputnik.Sputnik') >> '/opt/sputnik'
        }

        when:
        def configuration = configurationProvider.getConfiguration(variables, capabilityContext, new ConfigurationMapImpl())

        then:
        configuration.sputnikPath == '/opt/sputnik'
    }

    def 'prefer sputnik path from configuration over from capability context'() {
        given:
        Map<String, VariableDefinitionContext> variables = requiredVariables()
        DefaultConfigurationProvider configurationProvider = new DefaultConfigurationProvider()
        def capabilityContext = Stub(CapabilityContext) {
            getCapabilityValue('system.builder.sputnik.Sputnik') >> '/opt/sputnik'
        }
        def configurationMap = new ConfigurationMapImpl(['builder.sputnik.path': '/opt/sputnik-1.7.5'])

        when:
        def configuration = configurationProvider.getConfiguration(variables, capabilityContext, configurationMap)

        then:
        configuration.sputnikPath == '/opt/sputnik-1.7.5'
    }

    def 'ignore empty sputnik path from configuration'() {
        given:
        Map<String, VariableDefinitionContext> variables = requiredVariables()
        DefaultConfigurationProvider configurationProvider = new DefaultConfigurationProvider()
        def capabilityContext = Stub(CapabilityContext) {
            getCapabilityValue('system.builder.sputnik.Sputnik') >> '/opt/sputnik'
        }
        def configurationMap = new ConfigurationMapImpl(['builder.sputnik.path': ''])

        when:
        def configuration = configurationProvider.getConfiguration(variables, capabilityContext, configurationMap)

        then:
        configuration.sputnikPath == '/opt/sputnik'
    }

    @Unroll
    def "get extra option #optionKey=#optionValue from variables"() {
        given:
        def variableList = [aVariable('sputnik.connector.username', 'user1', GLOBAL),
                            aVariable('sputnik.connector.password', 'password1', GLOBAL),
                            aVariable("sputnik.${optionKey}", optionValue, GLOBAL)]
        Map<String, VariableDefinitionContext> variables = createVariables(variableList)
        DefaultConfigurationProvider configurationProvider = new DefaultConfigurationProvider()

        when:
        def configuration = configurationProvider.getConfiguration(variables, Stub(CapabilityContext), new ConfigurationMapImpl())

        then:
        configuration.properties[optionKey] == optionValue

        where:
        optionKey             | optionValue
        'connector.verifySsl' | 'false'
    }

    @Unroll
    def "get property #optionKey=#optionValue from configuration"() {
        given:
        Map<String, VariableDefinitionContext> variables = requiredVariables()
        DefaultConfigurationProvider configurationProvider = new DefaultConfigurationProvider()
        def configurationMap = new ConfigurationMapImpl(['builder.sputnik.config.body': "$optionKey=$optionValue".toString()])

        when:
        def configuration = configurationProvider.getConfiguration(variables, Stub(CapabilityContext), configurationMap)

        then:
        configuration.properties[optionKey] == optionValue

        where:
        optionKey            | optionValue
        'project.build.tool' | 'maven'
    }

    @Unroll
    def 'do not allow to set #propertyKey property by non global variable by default'() {
        given:
        def variableList = [aVariable('sputnik.connector.username', 'user1', GLOBAL),
                            aVariable('sputnik.connector.password', 'password1', GLOBAL),
                            aVariable("sputnik.${propertyKey}", propertyValue, PLAN)]
        Map<String, VariableDefinitionContext> variables = createVariables(variableList)
        DefaultConfigurationProvider configurationProvider = new DefaultConfigurationProvider()

        when:
        def configuration = configurationProvider.getConfiguration(variables, Stub(CapabilityContext), new ConfigurationMapImpl())

        then:
        configuration.properties[propertyKey] != propertyValue

        where:
        propertyKey           | propertyValue
        'connector.verifySsl' | 'false'
        'connector.useHttps'  | 'false'
    }

    @Unroll
    def 'allow to set #property property by non global variable'() {
        given:
        def variableList = [aVariable('sputnik.connector.username', 'user1', GLOBAL),
                            aVariable('sputnik.connector.password', 'password1', GLOBAL),
                            aVariable("sputnik.${property}.override", 'true', GLOBAL),
                            aVariable("sputnik.${property}", 'true', PLAN)]
        Map<String, VariableDefinitionContext> variables = createVariables(variableList)
        DefaultConfigurationProvider configurationProvider = new DefaultConfigurationProvider()

        when:
        def configuration = configurationProvider.getConfiguration(variables, Stub(CapabilityContext), new ConfigurationMapImpl())

        then:
        configuration.properties[property] == 'true'

        where:
        property << ['connector.verifySsl']
    }

    @Unroll
    def 'cannot set #property property'() {
        given:
        def variableList = [aVariable('sputnik.connector.username', 'user1', GLOBAL),
                            aVariable('sputnik.connector.password', 'password1', GLOBAL),
                            aVariable("sputnik.${property}", 'x', GLOBAL)]
        Map<String, VariableDefinitionContext> variables = createVariables(variableList)
        DefaultConfigurationProvider configurationProvider = new DefaultConfigurationProvider()

        when:
        def configuration = configurationProvider.getConfiguration(variables, Stub(CapabilityContext), new ConfigurationMapImpl())

        then:
        !configuration.properties.containsKey(property)

        where:
        property << ['connector.type',
                     'connector.host',
                     'connector.port',
                     'connector.path',
                     'connector.project',
                     'connector.repository']
    }

    @Unroll
    def "#option is 'true' by default"() {
        given:
        DefaultConfigurationProvider configurationProvider = new DefaultConfigurationProvider()
        Map<String, VariableDefinitionContext> variables = requiredVariables()

        when:
        def configuration = configurationProvider.getConfiguration(variables, Stub(CapabilityContext), new ConfigurationMapImpl())

        then:
        configuration.getProperties()[option] == 'true'

        where:
        option << ['connector.verifySsl', 'connector.useHttps']
    }

    def "throws exception on missing password property"() {
        given:
        DefaultConfigurationProvider configurationProvider = new DefaultConfigurationProvider()
        def variablesWithoutPassword = requiredVariables().findAll { it.key != 'sputnik.connector.password' }
        def capabilityContext = Stub(CapabilityContext) {
            getCapabilityValue('system.builder.sputnik.Sputnik') >> '/opt/sputnik'
        }

        when:
        configurationProvider.getConfiguration(variablesWithoutPassword, capabilityContext, new ConfigurationMapImpl())

        then:
        def e = thrown(TaskException)
        e.message.contains 'connector.password'
    }

    def "throws exception on missing username property"() {
        given:
        DefaultConfigurationProvider configurationProvider = new DefaultConfigurationProvider()
        def variablesWithoutUsername = requiredVariables().findAll { it.key != 'sputnik.connector.username' }
        def capabilityContext = Stub(CapabilityContext) {
            getCapabilityValue('system.builder.sputnik.Sputnik') >> '/opt/sputnik'
        }

        when:
        configurationProvider.getConfiguration(variablesWithoutUsername, capabilityContext, new ConfigurationMapImpl())

        then:
        def e = thrown(TaskException)
        e.message.contains 'connector.username'
    }

    def "throws exception on missing sputnik path property"() {
        given:
        DefaultConfigurationProvider configurationProvider = new DefaultConfigurationProvider()
        def variables = requiredVariables()
        def capabilityContext = Stub(CapabilityContext) {
            getCapabilityValue('system.builder.sputnik.Sputnik') >> null
        }

        when:
        configurationProvider.getConfiguration(variables, capabilityContext, new ConfigurationMapImpl())

        then:
        def e = thrown(TaskException)
        e.message == 'Neither Sputnik capability nor Sputnik path is defined!'
    }

    private static Map<String, VariableDefinitionContext> requiredVariables() {
        def variableList = [aVariable('sputnik.connector.username', 'user1', GLOBAL),
                            aVariable('sputnik.connector.password', 'password1', GLOBAL)]
        createVariables(variableList)
    }

    private static Map<String, VariableDefinitionContext> createVariables(List<SimpleVariableDefinitionContext> variableList) {
        variableList.collectEntries { [(it.key): it] }
    }

    private static aVariable(String key, String value, VariableType type) {
        new SimpleVariableDefinitionContext(key, value, type)
    }
}