/** @type {import('tailwindcss').Config} */
module.exports = {
  content: [
    "./src/**/*.{html,ts}",
  ],
  theme: {
    extend: {
      colors: {
        'neo-black': '#000000',
        'neo-white': '#FFFFFF',
        'neo-yellow': '#FFDD00',  // Updated to match the requirements
        'neo-blue': '#2B7FFF',    // Updated to match the requirements
        'neo-red': '#FF4B4B',     // Updated to match the requirements
        'neo-pink': '#FF499E',
        'neo-green': '#00BA88',
        'neo-orange': '#FF8A47',
        'neo-purple': '#7F5AF0',
        'neo-gray': '#F4F4F4',
      },
      fontFamily: {
        'display': ['Space Grotesk', 'sans-serif'],
        'body': ['Inter', 'sans-serif'],
      },
      boxShadow: {
        'neo': '4px 4px 0px #000000',
        'neo-lg': '6px 6px 0px #000000',
        'neo-xl': '8px 8px 0px #000000',
      },
      aspectRatio: {
        '6/19': '6 / 19', // Adding the 6:19 aspect ratio for videos as required
      },
    },
  },
  plugins: [
    require('@tailwindcss/forms'),
    // Line clamp is now included by default in Tailwind CSS v3.3+
    require('@tailwindcss/typography'),
  ],
}

