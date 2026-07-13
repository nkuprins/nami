package com.app.backend.util;

import org.junit.jupiter.api.Test;

import javax.xml.stream.XMLStreamException;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class XmlStreamTest {

    private static XmlStream open(String content, String itemLocalName) throws XMLStreamException {
        InputStream in = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
        return XmlStream.open(in, itemLocalName);
    }

    @Test
    void flattensNestedLeafFieldsAcrossRepeatingItems() throws XMLStreamException {
        try (XmlStream xml = open("""
                <?xml version="1.0" encoding="UTF-8"?>
                <BuildingFullData>
                <BuildingItemList>
                <BuildingItemData>
                <BuildingBasicData>
                <BuildingCadastreNr>21000030512001</BuildingCadastreNr>
                <VARISCode>6001</VARISCode>
                <BuildingUseKind><BuildingUseKindId>1122</BuildingUseKindId><BuildingUseKindName>Dzīvojamā māja</BuildingUseKindName></BuildingUseKind>
                <BuildingExploitYear>1985</BuildingExploitYear>
                </BuildingBasicData>
                <ObjectRelation><ObjectCadastreNr>21000030512</ObjectCadastreNr><ObjectType>PARCEL</ObjectType></ObjectRelation>
                </BuildingItemData>
                <BuildingItemData>
                <BuildingBasicData>
                <BuildingCadastreNr>21000030512002</BuildingCadastreNr>
                <BuildingExploitYear>1970</BuildingExploitYear>
                </BuildingBasicData>
                <ObjectRelation><ObjectCadastreNr>21000030513</ObjectCadastreNr><ObjectType>PARCEL</ObjectType></ObjectRelation>
                </BuildingItemData>
                </BuildingItemList>
                </BuildingFullData>
                """, "BuildingItemData")) {
            Map<String, String> first = xml.next();
            assertThat(first.get("BuildingCadastreNr")).isEqualTo("21000030512001");
            assertThat(first.get("VARISCode")).isEqualTo("6001");
            assertThat(first.get("BuildingUseKindId")).isEqualTo("1122");
            assertThat(first.get("BuildingExploitYear")).isEqualTo("1985");

            Map<String, String> second = xml.next();
            assertThat(second.get("BuildingCadastreNr")).isEqualTo("21000030512002");
            assertThat(second.get("VARISCode")).isNull();
            assertThat(second.get("BuildingExploitYear")).isEqualTo("1970");

            assertThat(xml.next()).isNull();
        }
    }

    @Test
    void ignoresElementsOutsideTheRequestedItemName() throws XMLStreamException {
        try (XmlStream xml = open("""
                <?xml version="1.0" encoding="UTF-8"?>
                <PremiseGroupFullData>
                <PreparedDate>2026-07-12</PreparedDate>
                <PremiseGroupItemList>
                <PremiseGroupItemData>
                <PremiseGroupBasicData>
                <PremiseGroupCadastreNr>21000030512001036</PremiseGroupCadastreNr>
                <PremiseGroupVARISCode>115610248</PremiseGroupVARISCode>
                <PremiseGroupArea>52.8</PremiseGroupArea>
                </PremiseGroupBasicData>
                </PremiseGroupItemData>
                </PremiseGroupItemList>
                </PremiseGroupFullData>
                """, "PremiseGroupItemData")) {
            Map<String, String> item = xml.next();
            assertThat(item.get("PremiseGroupCadastreNr")).isEqualTo("21000030512001036");
            assertThat(item.get("PremiseGroupVARISCode")).isEqualTo("115610248");
            assertThat(item.get("PremiseGroupArea")).isEqualTo("52.8");
            assertThat(item).doesNotContainKey("PreparedDate");
            assertThat(xml.next()).isNull();
        }
    }
}
