package org.grails.orm.hibernate.validation

import org.grails.orm.hibernate.*
import grails.persistence.Entity
import spock.lang.Issue
import spock.lang.Specification

/**
 * @author Graeme Rocher
 */
class InheritanceWithValidationSpec extends GormSpec{

    @Issue('GRAILS-10664')
    void "Test that validation works with inheritance"() {

        given:"a domain with an abstract single ended association"
            ContentText text = new ContentText()

            text.message = 'TEST'

            Document document = new Document()
            document.title = text
            document.save(flush:true)
            session.clear()

        when:"The instance is validated and modified"

            document = Document.get(document.id)

            document.title.message = "Modified !"

            document.save(flush:true)
            session.clear()

        then:"The update occurred correctly"
            Document.get(document.id).title.message == 'Modified !'


    }

    @Override
    List getDomainClasses() {
        [AbstractContent, ContentText, Document]
    }
}


@Entity
class Document {

    AbstractContent title

    static mapping = {
        title(cascade: 'all')
    }
}
@Entity
class AbstractContent {
}
@Entity
class ContentText extends AbstractContent {
    String message
}