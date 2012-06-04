import org.specs2._

object EntitySpec extends Specification { def is = sequential                 ^
  "this is a specification to check the generated entity source"              ^
                                                                              p^
  "the generated entity source should"                                        ^
    "start with // Generated by"                                              ! entity1^
    "produce package mapped to the target namespace"                          ! entity2^
                                                                              end^
  "xs:string should"                                                          ^
    "be referenced as String"                                                 ! builtin1^
                                                                              end^
  "restrictions of xs:positiveInteger should"                                 ^
    "be referenced as BigInt"                                                 ! restriction1^
                                                                              end^
  "restrictions of simple type should"                                        ^
    "be referenced as its base built-in type"                                 ! restriction2^
                                                                              end^
  "lists of a simple type should"                                             ^
    "be referenced as Seq of its base type"                                   ! derivation1^
                                                                              end^
  "unions of simple types should"                                             ^
    "be referenced as String"                                                 ! union1^
                                                                              end^
  "top-level simple types with enumeration should"                            ^
    "generate a trait named similarly"                                        ! enum1^
    "each enumerations represented as case object"                            ! enum2^
    "be referenced as the trait"                                              ! enum3^
                                                                              end^
  "top-level complex types should"                                            ^
    "generate a case class named similarly"                                   ! complexType1^
    "not generate case class for the primary sequence"                        ! complexType2^
    "be referenced as the class/trait"                                        ! complexType3^
    "be referenced as Option[A] if nillable"                                  ! complexType3^
    "be referenced as Option[A] if optional"                                  ! complexType3^
    "be referenced as Option[Option[A]] if nillable and optional"             ! complexType3^
    "be referenced as Seq[A] if maxOccurs >1"                                 ! complexType3^
    "be referenced as Seq[Option[A]] if nillable and maxOccurs >1"            ! complexType3^
                                                                              end^
  "top-level elements with a local complex type should"                       ^
    "generate a case class named similarly"                                   ! element1^
                                                                              end^
  "local elements with a local complex type should"                           ^
    "generate a case class named similarly"                                   ! localelement1^
                                                                              end^
  "top-level named group should"                                              ^
    "generate a case class named similarly"                                   ! group1^
                                                                              end^
  "sequences in a complex type should"                                        ^
    "generate a case class named FooSequence* for non-primary sequences"      ! seq1^
    "be referenced as fooSequence within the type"                            ! seq2^
    "not generate anything when the primary sequence is empty"                ! seq3^
    "generate a case class if the primary sequence is either optional or multiple" ! seq4^
    "be split into chunks of case classes when it exceeds 20 particles"       ! seq5^
    "generate accessors for elements within the wrapped sequence"             ! seq6^
                                                                              end^
  "choices in a complex type should"                                          ^
    "generate a trait named FooOption*"                                       ! choice1^
    "be referenced as DataRecord[FooOption] if it's made of non-nillable complex type element" ! choice2^
    "be referenced as DataRecord[Option[FooOption]] if it's made of complex types, some nillable" ! choice2^
    "be referenced as DataRecord[Int] if it's made of xs:int"                 ! choice2^
                                                                              end^
  "an all in a complex type should"                                           ^
    "be referenced as Map[String, scalaxb.DataRecord[Any]]"                   ! all1^
                                                                              end^
  "wildcards should"                                                          ^
    "be referenced as DataRecord[Any] named any*"                             ! wildcard1^
    "be referenced as Option[DataRecord[Any]] if optional"                    ! wildcard2^
    "be referenced as Seq[DataRecord[Any]] if maxOccurs >1"                   ! wildcard2^
                                                                              end^
  "a single particle with maxOccurs >1 should"                                ^
    "be referenced as A*"                                                     ! param1^
                                                                              end^
  "substitution groups should"                                                ^
    "be referenced as the group head's type"                                  ! sub1^
                                                                              end^
  "attriubtes should"                                                         ^
    "be referenced as Map[String, scalaxb.DataRecord[Any]]"                   ! attr1^
    "generate an accessor"                                                    ! attr2^
                                                                              end^
  "attribute groups should"                                                   ^
    "generate a trait with attribute accessor signatures"                     ! attributegroup1^
    "generate accessors in the referencing complex type"                      ! attributegroup2^
    "be extended by the referencing complex types"                            ! attributegroup3^
                                                                              end

  import Example._
  // scalaxb.compiler.Module.configureLogger(true)
  lazy val module = new scalaxb.compiler.xsd2.Driver
  lazy val emptyEntitySource = module.processNode(<xs:schema targetNamespace="http://www.example.com/"
    xmlns:xs="http://www.w3.org/2001/XMLSchema" />, "example1")(0)

  def entity1 = {
    println(emptyEntitySource)
    emptyEntitySource must startWith("// Generated by")
  }

  def entity2 = {
    emptyEntitySource must find("""package example1""".stripMargin)
  }

  val builtInEntitySource = module.processNode(<xs:schema targetNamespace="http://www.example.com/"
      xmlns:xs="http://www.w3.org/2001/XMLSchema">
    <xs:complexType name="Address">
      <xs:sequence>
        <xs:element name="street" type="xs:string"/>
        <xs:element name="city" type="xs:string"/>
      </xs:sequence>
    </xs:complexType>
  </xs:schema>, "example")(0)

  def builtin1 = {
    println(builtInEntitySource)
    builtInEntitySource must contain(
      """case class Address(street: String, city: String)""")
  }

  def restriction1 = {
    val entitySource = module.processNode(<xs:schema targetNamespace="http://www.example.com/"
        xmlns:xs="http://www.w3.org/2001/XMLSchema">
      <xs:complexType name="SimpleTypeTest">
        <xs:sequence>
          <xs:element name="quantity">
            <xs:simpleType>
              <xs:restriction base="xs:positiveInteger">
                <xs:maxExclusive value="100"/>
              </xs:restriction>
            </xs:simpleType>
          </xs:element>
        </xs:sequence>
      </xs:complexType>
    </xs:schema>, "example")(0)

    println(entitySource)
    entitySource must contain("""quantity: BigInt""")
  }

  def restriction2 = {
    val entitySource = module.processNode(<xs:schema targetNamespace="http://www.example.com/general"
        xmlns:xs="http://www.w3.org/2001/XMLSchema"
        xmlns:gen="http://www.example.com/general">
      <xs:simpleType name="ShortString">
        <xs:restriction base="xs:string">
          <xs:maxLength value="140"/>
        </xs:restriction>
      </xs:simpleType>
      <xs:complexType name="SimpleTypeTest">
        <xs:sequence>
          <xs:element name="comment">
            <xs:simpleType>
              <xs:restriction base="gen:ShortString">
                <xs:maxLength value="100"/>
              </xs:restriction>
            </xs:simpleType>
          </xs:element>
          <xs:element name="comment2">
            <xs:simpleType>
              <xs:restriction>
                <xs:simpleType>
                  <xs:restriction base="gen:ShortString">
                    <xs:maxLength value="130"/>
                  </xs:restriction>
                </xs:simpleType>
                <xs:maxLength value="100"/>
              </xs:restriction>
            </xs:simpleType>
          </xs:element>
        </xs:sequence>
      </xs:complexType>
    </xs:schema>, "example")(0)

    println(entitySource)
    entitySource must contain("""comment: String""") and contain("""comment2: String""")
  }

  def derivation1 = {
    val entitySource = module.processNode(<xs:schema targetNamespace="http://www.example.com"
        xmlns:xs="http://www.w3.org/2001/XMLSchema"
        xmlns:tns="http://www.example.com">
      <xs:complexType name="SimpleTypeTest">
        <xs:sequence>
          <xs:element name="milklist1" type="tns:ListOfMilk"/>
        </xs:sequence>
      </xs:complexType>

      <xs:simpleType name="ListOfMilk">
        <xs:list itemType="tns:MilkType"/>
      </xs:simpleType>

      <xs:simpleType name="MilkType">
        <xs:restriction base="xs:NMTOKEN">
          <xs:enumeration value="WHOLE"/>
          <xs:enumeration value="SKIM"/>
        </xs:restriction>
      </xs:simpleType>
    </xs:schema>, "example")(0)

    println(entitySource)
    entitySource must contain("""milklist1: Seq[example.MilkType]""")
  }

  def union1 = {
    val entitySource = module.processNode(<xs:schema targetNamespace="http://www.example.com/"
        xmlns:xs="http://www.w3.org/2001/XMLSchema"
        xmlns:tns="http://www.example.com/">
      <xs:complexType name="SimpleTypeTest">
        <xs:sequence>
          <xs:element name="union">
            <xs:simpleType>
              <xs:union memberTypes="xs:string xs:int" />
            </xs:simpleType>
          </xs:element>
        </xs:sequence>
      </xs:complexType>
    </xs:schema>, "example")(0)

    println(entitySource)
    entitySource must contain("""union: String""")
  }

  lazy val enumEntitySource = module.processNode(<xs:schema targetNamespace="http://www.example.com/"
      xmlns:tns="http://www.example.com/"
      xmlns:xs="http://www.w3.org/2001/XMLSchema">
    <xs:simpleType name="MilkType">
      <xs:restriction base="xs:NMTOKEN">
        <xs:enumeration value="WHOLE"/>
        <xs:enumeration value="SKIM"/>
      </xs:restriction>
    </xs:simpleType>
    <xs:complexType name="SimpleTypeTest">
      <xs:sequence>
        <xs:element name="milk1" type="tns:MilkType"/>
      </xs:sequence>
    </xs:complexType>
  </xs:schema>, "example")(0)

  def enum1 = {
    println(enumEntitySource)
    enumEntitySource must contain("""trait MilkType""")
  }

  def enum2 = {
    enumEntitySource must contain("""case object SKIM""")
  }

  def enum3 = {
    enumEntitySource must contain("""milk1: example.MilkType""")
  }

  lazy val complexTypeEntitySource = module.processNode(complexTypeCardinalityXML, "example")(0)

  lazy val expectedComplexTypeTest =
    """case class SingularComplexTypeTest(person1: example.Person, person2: Option[example.Person], """ +
      """person3: Option[example.Person], person4: Option[Option[example.Person]], """ +
      """person5: Seq[example.Person], person6: Seq[Option[example.Person]])"""

  def complexType1 = {
    println(complexTypeEntitySource)
    complexTypeEntitySource must contain("""case class Address(""")
  }

  def complexType2 = {
    complexTypeEntitySource must not contain("""AddressSequence""")
  }

  def complexType3 = {
    complexTypeEntitySource must contain(expectedComplexTypeTest)
  }

  def element1 = {
    val entitySource = module.processNode(<xs:schema targetNamespace="http://www.example.com/"
        xmlns:xs="http://www.w3.org/2001/XMLSchema">
      <xs:element name="topLevelElementTest">
        <xs:complexType>
          <xs:sequence>
            <xs:choice maxOccurs="unbounded">
              <xs:element name="foo" type="xs:string"/>
              <xs:any namespace="##other" processContents="lax" />
            </xs:choice>
          </xs:sequence>
        </xs:complexType>
      </xs:element>
    </xs:schema>, "example")(0)

    println(entitySource)
    entitySource must contain("""case class TopLevelElementTest(""")
  }

  def localelement1 = {
    val entitySource = module.processNode(<xs:schema targetNamespace="http://www.example.com/ipo"
        xmlns:xs="http://www.w3.org/2001/XMLSchema"
        xmlns:ipo="http://www.example.com/ipo">
      <xs:element name="comment" type="xs:string"/>

      <xs:complexType name="Items">
        <xs:sequence>
          <xs:element name="item" minOccurs="0" maxOccurs="unbounded">
            <xs:complexType>
              <xs:sequence>
                <xs:element name="productName" type="xs:string"/>
                <xs:element name="quantity">
                  <xs:simpleType>
                    <xs:restriction base="xs:positiveInteger">
                      <xs:maxExclusive value="100"/>
                    </xs:restriction>
                  </xs:simpleType>
                </xs:element>
                <xs:element name="USPrice"    type="xs:decimal"/>
                <xs:element ref="ipo:comment" minOccurs="0"/>
                <xs:element name="shipDate"   type="xs:date" minOccurs="0"/>
              </xs:sequence>
              <xs:attribute name="partNum" type="xs:int" use="required"/>
            </xs:complexType>
          </xs:element>
        </xs:sequence>
      </xs:complexType>
    </xs:schema>, "example")(0)

    println(entitySource)
    (entitySource must contain("""case class Items(item: example.Item*)""")) and
    (entitySource must contain("""case class Item("""))
  }

  def group1 = {
    val entitySource = module.processNode(<xs:schema targetNamespace="http://www.example.com/ipo"
        xmlns:xs="http://www.w3.org/2001/XMLSchema"
        xmlns:ipo="http://www.example.com/ipo">
      <xs:group name="emptySeqGroup">
        <xs:sequence/>
      </xs:group>
      <xs:group name="seqGroup">
        <xs:sequence>
          <xs:element name="city" type="xs:string"/>
        </xs:sequence>
      </xs:group>      
    </xs:schema>, "example")(0)

    println(entitySource)
    (entitySource must contain("""case class EmptySeqGroupSequence""")) and
    (entitySource must contain("""case class SeqGroupSequence(city: String)"""))    
  }

  lazy val seqEntitySource = module.processNode(sequenceXML, "example")(0)

  val seqExpectedSequenceTest =
    """case class SequenceComplexTypeTest(sequencecomplextypetestsequence: example.SequenceComplexTypeTestSequence, """ +
      """sequencecomplextypetestsequence2: example.SequenceComplexTypeTestSequence2, """ +
      """sequencecomplextypetestsequence3: Option[example.SequenceComplexTypeTestSequence3], """ +
      """sequencecomplextypetestsequence4: Option[example.SequenceComplexTypeTestSequence4], """ +
      """sequencecomplextypetestsequence5: Seq[example.SequenceComplexTypeTestSequence5], """ +
      """sequencecomplextypetestsequence6: Seq[example.SequenceComplexTypeTestSequence6], """ +
      """sequencecomplextypetestsequence7: example.SequenceComplexTypeTestSequence7)"""

  def seq1 = {
    println(seqEntitySource)
    seqEntitySource must contain("""case class SequenceComplexTypeTestSequence""")
  }

  def seq2 = {
    seqEntitySource must contain(seqExpectedSequenceTest)
  }

  def seq3 = {
    seqEntitySource must contain("""case class EmptySequenceComplexTypeTest""")
  }

  def seq4 = {
    seqEntitySource must contain("""case class MultipleSequenceComplexTypeTest(""" +
      """multiplesequencecomplextypetestsequence: example.MultipleSequenceComplexTypeTestSequence*)""")
  }

  def seq5 = {
    seqEntitySource must contain("""case class LongSequenceComplexTypeTestSequence(int1: Int""")
  }

  def seq6 = {
    seqEntitySource must contain("""case class LongSequenceComplexTypeTestSequence(int1: Int""")
  }

  lazy val choiceEntitySource = module.processNode(choiceXML, "example")(0)

  val expectedChoiceTest =
    """case class ChoiceComplexTypeTest(choicecomplextypetestoption: scalaxb.DataRecord[example.ChoiceComplexTypeTestOption], """ +
      """choicecomplextypetestoption2: scalaxb.DataRecord[Option[example.ChoiceComplexTypeTestOption2]], """ +
      """choicecomplextypetestoption3: Option[scalaxb.DataRecord[example.ChoiceComplexTypeTestOption3]], """ +
      """choicecomplextypetestoption4: Option[scalaxb.DataRecord[Option[example.ChoiceComplexTypeTestOption4]]], """ +
      """choicecomplextypetestoption5: Seq[scalaxb.DataRecord[example.ChoiceComplexTypeTestOption5]], """ +
      """choicecomplextypetestoption6: Seq[scalaxb.DataRecord[Option[example.ChoiceComplexTypeTestOption6]]], """ +
      """choicecomplextypetestoption7: scalaxb.DataRecord[Int])"""

  def choice1 = {
    println(choiceEntitySource)
    choiceEntitySource must contain("""trait ChoiceComplexTypeTestOption""")
  }

  def choice2 = {
    choiceEntitySource must contain(expectedChoiceTest)
  }

  def all1 = {
    val entitySource = module.processNode(allXML, "example")(0)

    println(entitySource)
    entitySource must contain("""case class AllComplexTypeTest(all: Map[String, scalaxb.DataRecord[Any]])""")
  }

  lazy val wildcardEntitySource = module.processNode(wildcardXML, "example")(0)

  val exptectedAnyTest =
    """case class WildcardTest(person1: example.Person, """ +
      """any: scalaxb.DataRecord[Any], """ +
      """any2: scalaxb.DataRecord[Any], """ +
      """any3: scalaxb.DataRecord[Any], """ +
      """any4: scalaxb.DataRecord[Any], """ +
      """wildcardtestoption: scalaxb.DataRecord[scalaxb.DataRecord[Any]], """ +
      """person3: Option[example.Person], """ +
      """any5: Option[scalaxb.DataRecord[Any]], """ +
      """wildcardtestoption2: Option[scalaxb.DataRecord[scalaxb.DataRecord[Any]]], """ +
      """any6: Seq[scalaxb.DataRecord[Any]], """ +
      """person5: Seq[example.Person], """ +
      """any7: scalaxb.DataRecord[Any])"""
  
  def wildcard1 = {
    println(wildcardEntitySource)
    wildcardEntitySource must contain(exptectedAnyTest)
  }

  def wildcard2 = {
    wildcardEntitySource must contain(exptectedAnyTest)
  }

  def param1 = {
    val entitySource = module.processNode(seqParamXML, "example")(0)

    println(entitySource)
    entitySource.lines.toList must contain(
      """case class SeqParamTest(foo: String*)""",
      """case class NillableSeqParamTest(foo: Option[String]*)"""
    )
  }

  def sub1 = {
    val entitySource = module.processNode(<xs:schema targetNamespace="http://www.example.com/general"
        xmlns:xs="http://www.w3.org/2001/XMLSchema"
        xmlns:gen="http://www.example.com/general">
      <xs:simpleType name="MilkType">
        <xs:restriction base="xs:NMTOKEN">
          <xs:enumeration value="WHOLE"/>
          <xs:enumeration value="SKIM"/>
        </xs:restriction>
      </xs:simpleType>

      <xs:element name="SubstitutionGroup" type="xs:anyType" abstract="true"/>
      <xs:element name="SubGroupMember" type="gen:MilkType" substitutionGroup="gen:SubstitutionGroup"/>
      <xs:element name="SubGroupMember2" type="xs:int" substitutionGroup="gen:SubstitutionGroup"/>

      <xs:complexType name="SubstitutionGroupTest">
        <xs:sequence>
          <xs:element ref="gen:SubstitutionGroup"/>
        </xs:sequence>
      </xs:complexType>
    </xs:schema>, "example")(0)

    println(entitySource)
    entitySource must contain("""case class SubstitutionGroupTest(SubstitutionGroup: scalaxb.DataRecord[Any])""")
  }

  lazy val attrEntitySource = module.processNode(<xs:schema targetNamespace="http://www.example.com/general"
        xmlns:xs="http://www.w3.org/2001/XMLSchema"
        xmlns:gen="http://www.example.com/general">
      <xs:simpleType name="MilkType">
        <xs:restriction base="xs:NMTOKEN">
          <xs:enumeration value="WHOLE"/>
          <xs:enumeration value="SKIM"/>
        </xs:restriction>
      </xs:simpleType>

      <xs:element name="attributeTest">
        <xs:complexType>
          <xs:attribute name="milk1" type="gen:MilkType"/>
          <xs:attribute name="string2" type="xs:string"/>
          <xs:anyAttribute namespace="##any"/>
        </xs:complexType>
      </xs:element>

      <xs:complexType name="anySimpleTypeExtension">
        <xs:simpleContent>
          <xs:extension base="xs:anySimpleType">
             <xs:anyAttribute namespace="##any" processContents="lax"/>
          </xs:extension>
        </xs:simpleContent>
      </xs:complexType>
    </xs:schema>, "example")(0)

  def attr1 = {
    println(attrEntitySource)
    attrEntitySource must contain("""case class AttributeTest(attributes: Map[String, scalaxb.DataRecord[Any]])""")
  }

  def attr2 = {
    attrEntitySource must contain("""lazy val milk1: Option[example.MilkType] = attributes.get("@milk1").map(_.as[example.MilkType])""")
  }

  lazy val attributeGroupEntitySource = module.processNode(<xs:schema targetNamespace="http://www.example.com/general"
        xmlns:xs="http://www.w3.org/2001/XMLSchema"
        xmlns:gen="http://www.example.com/general">
      <xs:attributeGroup name="coreattrs">
        <xs:attribute name="id" type="xs:ID"/>
        <xs:attribute name="class" type="xs:NMTOKENS"/>
      </xs:attributeGroup>

      <xs:element name="attributeGroupTest">
        <xs:complexType>
          <xs:attributeGroup ref="gen:coreattrs"/>
        </xs:complexType>
      </xs:element>
    </xs:schema>, "example")(0)

  def attributegroup1 = {
    println(attributeGroupEntitySource)
    attributeGroupEntitySource.lines.toList must contain(
      """trait Coreattrs {""",
      """  def id: Option[String]""").inOrder
  }

  def attributegroup2 = {
    attributeGroupEntitySource must contain("""lazy val id: Option[String] = attributes.get("@id").map(_.as[String])""")
  }

  def attributegroup3 = {
    attributeGroupEntitySource must contain("""case class AttributeGroupTest(attributes: Map[String, scalaxb.DataRecord[Any]]) extends example.Coreattrs""")
  }
}
