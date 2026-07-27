package com.example.mobileassignmentloginpart

import android.os.Bundle
import androidx.annotation.DrawableRes
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.res.painterResource

data class Recipe(
    val recipeId: String,
    val recipeName: String,
    val recipeDescription: String,
    val budget: Double,
    val skillLevel: Int,      // 1 = Beginner, 2 = Basic, 3 = Intermediate, 4 = Advanced, 5 = Expert
    val time: Int,
    val calories: Int,
    @DrawableRes val recipeImage: Int,
    val recipeStep: String,
//    val category: IngCat,
//    val ingList: List<IngredientUnits>
) {
    companion object {
        val dummyRecipes = listOf(
            Recipe(
                recipeId = "R001",
                recipeName = "Classic Tomato Pasta",
                recipeDescription = "A quick and comforting Italian classic made with fresh tomatoes, garlic, and basil.",
                budget = 5.50,
                skillLevel = 1, // Beginner
                time = 20,
                calories = 200,
                recipeImage = R.drawable.food,
                recipeStep = "1. Boil pasta in salted water.\n2. Sauté garlic in olive oil, then add crushed tomatoes.\n3. Simmer sauce for 10 mins.\n4. Toss pasta with the sauce and garnish with basil."
            ),
            Recipe(
                recipeId = "R002",
                recipeName = "Fluffy Buttermilk Pancakes",
                recipeDescription = "Golden, diner-style pancakes that are incredibly fluffy and perfect for weekend breakfast.",
                budget = 4.00,
                skillLevel = 2, // Basic
                time = 15,
                calories = 200,
                recipeImage = R.drawable.food,
                recipeStep = "1. Whisk flour, sugar, baking powder, and salt.\n2. Mix buttermilk, melted butter, and egg in another bowl.\n3. Combine wet and dry ingredients gently.\n4. Cook ¼ cup scoops on a hot griddle until golden brown."
            ),
            Recipe(
                recipeId = "R003",
                recipeName = "Creamy Chicken Alfredo",
                recipeDescription = "Rich and indulgent fettuccine tossed in a homemade creamy parmesan sauce with seared chicken.",
                budget = 12.80,
                skillLevel = 3, // Intermediate
                time = 35,
                calories = 200,
                recipeImage = R.drawable.food,
                recipeStep = "1. Cook fettuccine according to package instructions.\n2. Season chicken breast and sear in a skillet until cooked through; slice it.\n3. In the same skillet, melt butter, add heavy cream, and whisk in parmesan cheese until smooth.\n4. Toss pasta and chicken into the sauce."
            ),
            Recipe(
                recipeId = "R004",
                recipeName = "Beef Wellington",
                recipeDescription = "A luxurious centerpiece featuring tender beef fillet wrapped in puff pastry, mushroom duxelles, and prosciutto.",
                budget = 35.00,
                calories = 200,
                skillLevel = 4, // Advanced
                time = 90,
                recipeImage = R.drawable.food,
                recipeStep = "1. Sear beef fillet on all sides and brush with mustard.\n2. Finely chop mushrooms and cook down into a dry paste (duxelles).\n3. Layer prosciutto, mushroom paste, and beef on plastic wrap, roll tightly, and chill.\n4. Wrap the log in puff pastry, brush with egg wash, and bake at 200°C for 30 mins."
            ),
            Recipe(
                recipeId = "R005",
                recipeName = "Traditional French Macarons",
                recipeDescription = "Delicate, almond-meringue cookie shells sandwiched together with a smooth chocolate ganache.",
                budget = 8.50,
                skillLevel = 5, // Expert
                time = 120,
                calories = 200,
                recipeImage = R.drawable.food,
                recipeStep = "1. Sift almond flour and powdered sugar together.\n2. Whip egg whites and granulated sugar into stiff, glossy peaks.\n3. Fold ingredients carefully to reach a 'lava-like' consistency.\n4. Pipe onto baking sheets, let sit for 45 mins to form a skin, then bake at 150°C."
            ),
            Recipe(
                recipeId = "R006",
                recipeName = "Grilled Chicken Salad",
                recipeDescription = "A healthy salad with grilled chicken breast, fresh vegetables, and a light vinaigrette.",
                budget = 8.00,
                skillLevel = 2, // Basic
                time = 25,
                calories = 320,
                recipeImage = R.drawable.food,
                recipeStep = "1. Grill the chicken.\n2. Chop the vegetables.\n3. Mix everything together.\n4. Drizzle with vinaigrette."
            ),

            Recipe(
                recipeId = "R007",
                recipeName = "Vegetable Fried Rice",
                recipeDescription = "A simple fried rice packed with colorful vegetables and eggs.",
                budget = 4.50,
                skillLevel = 1, // Beginner
                time = 20,
                calories = 380,
                recipeImage = R.drawable.food,
                recipeStep = "1. Scramble the eggs.\n2. Stir-fry vegetables.\n3. Add cooked rice.\n4. Season and serve."
            ),

            Recipe(
                recipeId = "R008",
                recipeName = "Beef Stir Fry",
                recipeDescription = "Tender beef slices stir-fried with broccoli and carrots in a savory sauce.",
                budget = 9.50,
                skillLevel = 3, // Intermediate
                time = 30,
                calories = 450,
                recipeImage = R.drawable.food,
                recipeStep = "1. Marinate the beef.\n2. Stir-fry vegetables.\n3. Cook beef until browned.\n4. Mix with sauce."
            ),

            Recipe(
                recipeId = "R009",
                recipeName = "Creamy Mushroom Soup",
                recipeDescription = "A rich and creamy mushroom soup perfect as an appetizer or light meal.",
                budget = 6.00,
                skillLevel = 2, // Basic
                time = 35,
                calories = 250,
                recipeImage = R.drawable.food,
                recipeStep = "1. Sauté mushrooms and onions.\n2. Add stock.\n3. Blend until smooth.\n4. Stir in cream."
            ),

            Recipe(
                recipeId = "R010",
                recipeName = "Teriyaki Salmon",
                recipeDescription = "Pan-seared salmon glazed with homemade teriyaki sauce.",
                budget = 12.00,
                skillLevel = 4, // Advanced
                time = 30,
                calories = 480,
                recipeImage = R.drawable.food,
                recipeStep = "1. Season salmon.\n2. Pan-sear until cooked.\n3. Add teriyaki sauce.\n4. Serve with rice."
            ),

            Recipe(
                recipeId = "R011",
                recipeName = "Chicken Wrap",
                recipeDescription = "A delicious wrap filled with grilled chicken, lettuce, tomatoes, and cheese.",
                budget = 5.80,
                skillLevel = 1, // Beginner
                time = 15,
                calories = 340,
                recipeImage = R.drawable.food,
                recipeStep = "1. Grill the chicken.\n2. Prepare vegetables.\n3. Fill tortilla.\n4. Roll and serve."
            ),

            Recipe(
                recipeId = "R012",
                recipeName = "Spicy Korean Ramen",
                recipeDescription = "Instant ramen upgraded with vegetables, egg, and spicy Korean flavors.",
                budget = 4.00,
                skillLevel = 2, // Basic
                time = 15,
                calories = 520,
                recipeImage = R.drawable.food,
                recipeStep = "1. Cook noodles.\n2. Add vegetables.\n3. Crack in an egg.\n4. Serve hot."
            ),

            Recipe(
                recipeId = "R013",
                recipeName = "Shrimp Garlic Butter",
                recipeDescription = "Juicy shrimp cooked in garlic butter with a squeeze of fresh lemon.",
                budget = 11.00,
                skillLevel = 3, // Intermediate
                time = 20,
                calories = 360,
                recipeImage = R.drawable.food,
                recipeStep = "1. Melt butter.\n2. Sauté garlic.\n3. Cook shrimp.\n4. Finish with lemon juice."
            ),

            Recipe(
                recipeId = "R014",
                recipeName = "Avocado Toast",
                recipeDescription = "Crispy toast topped with mashed avocado, cherry tomatoes, and black pepper.",
                budget = 3.80,
                skillLevel = 1, // Beginner
                time = 10,
                calories = 280,
                recipeImage = R.drawable.food,
                recipeStep = "1. Toast bread.\n2. Mash avocado.\n3. Spread on toast.\n4. Add tomatoes and seasoning."
            ),

            Recipe(
                recipeId = "R015",
                recipeName = "Thai Green Curry",
                recipeDescription = "A fragrant Thai green curry with chicken, coconut milk, and vegetables.",
                budget = 10.50,
                skillLevel = 5, // Expert
                time = 45,
                calories = 550,
                recipeImage = R.drawable.food,
                recipeStep = "1. Fry curry paste.\n2. Add coconut milk.\n3. Cook chicken and vegetables.\n4. Simmer and serve with rice."
            ),
        )
    }
}