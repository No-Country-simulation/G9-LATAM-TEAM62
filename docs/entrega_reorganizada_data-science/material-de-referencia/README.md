# Material de referencia — NO va en ninguna rama

Estos dos scripts no forman parte de ninguna de las dos entregas a Backend — quedan acá como contexto/histórico del trabajo de Ciencia de Datos:

- **`generar_dataset.py`** — genera el dataset simulado (2,200 usuarios, ~105k transacciones) usado para futuros experimentos. No se conecta a nada real.
- **`budget_recommendation_engine.py`** — la versión en Python del Motor de Recomendaciones Presupuestarias, con el mismo comportamiento que `BudgetRecommendationService.java` (que sí va en la Rama 1). Se mantiene acá por si alguien de Ciencia de Datos quiere probar la lógica sin levantar el backend completo — no se sube a ningún lado del repositorio de Backend.

Si en algún momento se decide dockerizar herramientas internas del equipo de datos (fuera del alcance de Backend), estos dos scripts serían el punto de partida.
