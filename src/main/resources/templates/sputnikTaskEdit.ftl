[@ww.textarea labelKey='builder.sputnik.config.body' name='builder.sputnik.config.body' 
    rows='10' wrap='off' cssClass='long-field' /]
[@ui.bambooSection titleKey='repository.advanced.option' collapsible=true]
    [@ww.textfield labelKey='builder.sputnik.path' name='builder.sputnik.path' cssClass='long-field'
        descriptionKey='builder.sputnik.path.description' /]
    [@ww.textfield labelKey='builder.common.env' name='builder.sputnik.environmentVariables' cssClass='long-field' /]
    [@ww.checkbox labelKey='builder.sputnik.fail' name='builder.sputnik.fail' toggle='false' /]
[/@ui.bambooSection]

