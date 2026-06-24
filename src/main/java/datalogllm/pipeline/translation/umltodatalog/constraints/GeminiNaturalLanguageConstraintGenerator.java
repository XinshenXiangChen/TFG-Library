package datalogllm.pipeline.translation.umltodatalog.constraints;

import com.google.genai.Client;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;

import java.util.Objects;

public final class GeminiNaturalLanguageConstraintGenerator implements NaturalLanguageConstraintGenerator {

    private static final String DEFAULT_MODEL = "gemini-2.5-flash";

    private final Client client;
    private final String model;

    public GeminiNaturalLanguageConstraintGenerator(String apiKey) {
        this(apiKey, DEFAULT_MODEL);
    }

    public GeminiNaturalLanguageConstraintGenerator(String apiKey, String model) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalArgumentException("apiKey must not be null or blank");
        }
        this.client = Client.builder().apiKey(apiKey).build();
        this.model = (model == null || model.isBlank()) ? DEFAULT_MODEL : model;
    }

    @Override
    public String generateConstraints(String structuralDatalog, String naturalLanguageConstraints) {
        Objects.requireNonNull(structuralDatalog, "structuralDatalog must not be null");
        Objects.requireNonNull(naturalLanguageConstraints, "naturalLanguageConstraints must not be null");
        if (naturalLanguageConstraints.isBlank()) {
            return "";
        }

        String prompt = """
            You are given:
            1) Structural Datalog generated from UML classes/associations.
            2) Natural-language constraints.

            Generate ONLY additional Datalog constraints for the natural-language part.

            Rules:
            - Reuse existing predicates and arities from structural Datalog.
            - Do not redefine class/association schema comments.
            - Output plain text only (no markdown, no explanations).
            - Constraints must use format '@N :- ...'
            - If helpful, you may define auxiliary predicates.
            - Use safe, deterministic names for auxiliary predicates.
            
            Use the following schema as an exampleÑ
            
            
              @24 :- Ticket(T0,P0,C0), Ticket(T0,P1,C1), P0<>P1
              @25 :- Ticket(T0,P0,C0), Ticket(T0,P1,C1), C0<>C1
              @29 :- Concert(X), Person(X)
              @30 :- Concert(X), Ticket(X,P0,C0)
              @31 :- Person(X), Ticket(X,P0,C0)
            
            
              @42 :- Rating(X), not(RatingTicketHierarchyAux(X))
            
               RatingTicketHierarchyAux(X) :- Ticket(X,P0,C0)
    
            
              @32 :- IsParentOf(P0,P1), not(Person(P0))
              @33 :- IsParentOf(P0,P1), not(Person(P1))
              @35 :- Sings(C0,P0), not(Concert(C0))
              @36 :- Sings(C0,P0), not(Person(P0))
              @39 :- Ticket(OID,P0,C0), not(Person(P0))
              @40 :- Ticket(OID,P0,C0), not(Concert(C0))
              @41 :- Ticket(OID1,P0,C0), Ticket(OID2,P0,C0), OID1<>OID2
    
              MinSinger(C0) :- Sings(C0,P0)
            
              @37 :- Concert(C0), not(MinSinger(C0))
            
              @34 :- IsParentOf(P0,P1), IsParentOf(P0,P2), IsParentOf(P0,P3), P2<>P1, P3<>P2, P3<>P1
              @38 :- Sings(C0,P0), Sings(C0,P1), P1<>P0
            
            
              @0 :- ConcertName(C0,N0), not(Concert(C0))
              @3 :- ConcertDate(C0,D0), not(Concert(C0))
              @6 :- ConcertMinPrice(C0,M0), not(Concert(C0))
              @9 :- ConcertMaxPrice(C0,MA0), not(Concert(C0))
              @12 :- PersonName(P0,NA0), not(Person(P0))
              @15 :- PersonAddress(P0,A0), not(Person(P0))
              @18 :- PersonBirthday(P0,B0), not(Person(P0))
              @21 :- TicketPrice(T0,PR0), not(priceAux(T0))
              @26 :- RatingSatisfactionDegree(R0,SA0), not(satisfactionDegreeAux(R0))
              priceAux(T0) :- Ticket(T0,P0,C0)
              satisfactionDegreeAux(R0) :- Rating(R0)
                        
              MinConcertName(C0) :- ConcertName(C0,N1)
              MinConcertDate(C0) :- ConcertDate(C0,D1)
              MinConcertMinPrice(C0) :- ConcertMinPrice(C0,M1)
              MinConcertMaxPrice(C0) :- ConcertMaxPrice(C0,MA1)
              MinPersonName(P0) :- PersonName(P0,NA1)
              MinPersonAddress(P0) :- PersonAddress(P0,A1)
              MinPersonBirthday(P0) :- PersonBirthday(P0,B1)
              MinTicketPrice(T0) :- TicketPrice(T0,PR1)
              MinRatingSatisfactionDegree(R0) :- RatingSatisfactionDegree(R0,SA1)
            
              @1 :- Concert(C0), not(MinConcertName(C0))
              @4 :- Concert(C0), not(MinConcertDate(C0))
              @7 :- Concert(C0), not(MinConcertMinPrice(C0))
              @10 :- Concert(C0), not(MinConcertMaxPrice(C0))
              @13 :- Person(P0), not(MinPersonName(P0))
              @16 :- Person(P0), not(MinPersonAddress(P0))
              @19 :- Person(P0), not(MinPersonBirthday(P0))
              @22 :- Ticket(T0,P0,C0), not(MinTicketPrice(T0))
              @27 :- Rating(R0), not(MinRatingSatisfactionDegree(R0))
            
              @2 :- ConcertName(C0,N1), ConcertName(C0,N2), N2<>N1
              @5 :- ConcertDate(C0,D1), ConcertDate(C0,D2), D2<>D1
              @8 :- ConcertMinPrice(C0,M1), ConcertMinPrice(C0,M2), M2<>M1
              @11 :- ConcertMaxPrice(C0,MA1), ConcertMaxPrice(C0,MA2), MA2<>MA1
              @14 :- PersonName(P0,NA1), PersonName(P0,NA2), NA2<>NA1
              @17 :- PersonAddress(P0,A1), PersonAddress(P0,A2), A2<>A1
              @20 :- PersonBirthday(P0,B1), PersonBirthday(P0,B2), B2<>B1
              @23 :- TicketPrice(T0,PR1), TicketPrice(T0,PR2), PR2<>PR1
              @28 :- RatingSatisfactionDegree(R0,SA1), RatingSatisfactionDegree(R0,SA2), SA2<>SA1
                        
              @50 :- Concert(C1), Concert(C2), C1<>C2, ConcertName(C1,N0), ConcertName(C2,N0)
            
              @49 :- Person(P1), Person(P2), P1<>P2, PersonName(P1,NA0), PersonName(P2,NA0)
            
              @48 :- Concert(C0), ConcertMinPrice(C0,M0), M0<=10, Concert(C0)
            
              @47 :- Concert(C0), ConcertMaxPrice(C0,MA0), MA0>=100, Concert(C0)
            
              @46 :- Concert(C0), ConcertMinPrice(C0,M0), ConcertMaxPrice(C0,MA0), M0>=MA0, Concert(C0)
            
              @45 :- Ticket(T0,P1,C1), TicketPrice(T0,PR0), PR0<=100, Ticket(T0,P0,C0)
            
              @44 :- Ticket(R0,P0,C0), RatingSatisfactionDegree(R0,SA0), SA0<=10, Rating(R0)
            
              @43 :- Concert(C0), Ticket(T0,P0,C0), Rating(T0), not(AuxCardinalitynoBestFan1(C0,T0,T0,P0,C0)), Concert(C0)
              AuxCardinalitynoBestFan1(C0,T0,T0,P0,C0) :- Concert(C0), Ticket(T1,P1,C0), Rating(T1), RatingSatisfactionDegree(T1,SA0), Ticket(T0,P0,C0), Rating(T0), RatingSatisfactionDegree(T0,SA1), SA0>SA1
            
            

            Structural Datalog:
            ---
            %s
            ---

            Natural-language constraints:
            ---
            %s
            ---
            """.formatted(structuralDatalog, naturalLanguageConstraints);

        System.out.println("=== Gemini NL Constraint Request ===");
        System.out.println(prompt);
        System.out.println("=== End Gemini Request ===");

        GenerateContentConfig config = GenerateContentConfig.builder()
            .temperature(0.1F)
            .build();

        GenerateContentResponse response = client.models.generateContent(model, prompt, config);
        String text = response.text();
        String output = text == null ? "" : text.trim();

        System.out.println("=== Gemini NL Constraint Response ===");
        System.out.println(output);
        System.out.println("=== End Gemini Response ===");

        return output;
    }
}
