document.addEventListener("DOMContentLoaded", async () => {
    const response = await fetch("/mm");
    const games = await response.json();
    const listofgames = document.getElementById("listofgames");

    games.forEach(game => {
        const li = document.createElement("li");
        const a = document.createElement("a");

        a.href = "game.html?id=" + encodeURIComponent(game);
        a.textContent = game;

        li.appendChild(a);
        listofgames.appendChild(li);
    });
});